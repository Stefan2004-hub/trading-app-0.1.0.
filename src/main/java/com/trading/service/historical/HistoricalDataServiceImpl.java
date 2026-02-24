package com.trading.service.historical;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.dto.historical.HistoricalAssetRefreshStatusResponse;
import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.dto.historical.HistoricalSyncStartResponse;
import com.trading.dto.historical.SkippedAssetSyncItem;
import com.trading.service.historical.coingecko.CoinGeckoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "app.historical.enabled", havingValue = "true", matchIfMissing = true)
public class HistoricalDataServiceImpl implements HistoricalDataService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataServiceImpl.class);
    private static final int DEFAULT_LOOKBACK_DAYS = 30;
    private static final long EXTREME_THROTTLE_DELAY_MS = 60000L;

    private final AssetRepository assetRepository;
    private final AssetHistoricDataRepository assetHistoricDataRepository;
    private final CoinGeckoClient coinGeckoClient;
    private final HistoricalSyncProperties historicalSyncProperties;
    private final HistoricalSyncAsyncRunner historicalSyncAsyncRunner;
    private final AtomicBoolean extremeSyncInProgress = new AtomicBoolean(false);

    public HistoricalDataServiceImpl(
        AssetRepository assetRepository,
        AssetHistoricDataRepository assetHistoricDataRepository,
        CoinGeckoClient coinGeckoClient,
        HistoricalSyncProperties historicalSyncProperties,
        HistoricalSyncAsyncRunner historicalSyncAsyncRunner
    ) {
        this.assetRepository = assetRepository;
        this.assetHistoricDataRepository = assetHistoricDataRepository;
        this.coinGeckoClient = coinGeckoClient;
        this.historicalSyncProperties = historicalSyncProperties;
        this.historicalSyncAsyncRunner = historicalSyncAsyncRunner;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoricalDataRowResponse> listForUser(UUID userId, int page, int size) {
        return assetHistoricDataRepository.findAllForUserInvestedAssets(userId, PageRequest.of(page, size))
            .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalAssetRefreshStatusResponse> listAssetsNeedingRefreshToday(UUID userId) {
        LocalDate syncCutoffDate = syncCutoffDateUtc();
        return assetRepository.findAssetsMissingHistoricalDataForDay(syncCutoffDate).stream()
            .map((asset) -> new HistoricalAssetRefreshStatusResponse(
                asset.getId(),
                asset.getSymbol(),
                asset.getName(),
                syncCutoffDate
            ))
            .toList();
    }

    @Override
    public HistoricalSyncStartResponse startExtremeSync(UUID userId) {
        if (!extremeSyncInProgress.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Historical sync already running.");
        }
        historicalSyncAsyncRunner.runExtremeSync(() -> runExtremeSyncInBackground(userId));
        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return new HistoricalSyncStartResponse("STARTED", "Sync Started", startedAt);
    }

    @Override
    @Transactional
    public HistoricalDataSyncResponse syncIncremental(UUID userId, UUID assetId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Asset> assets = resolveAssetsToSync(assetId);
        if (assetId == null) {
            assetHistoricDataRepository.deleteByDayDate(today);
        } else {
            assetHistoricDataRepository.deleteByDayDateAndAsset_Id(today, assetId);
        }

        int rowsInserted = 0;
        int skippedExistingRecords = 0;
        List<SkippedAssetSyncItem> skippedAssets = new ArrayList<>();
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            AssetSyncResult result = syncAsset(asset, null);
            rowsInserted += result.rowsInserted();
            skippedExistingRecords += result.skippedExistingRecords();
            if (result.assetSkippedReason() != null) {
                skippedAssets.add(new SkippedAssetSyncItem(asset.getId(), asset.getSymbol(), result.assetSkippedReason()));
            }
            sleepAfterBatchIfNeeded(i + 1, assets.size());
        }
        log.info(
            "Sync complete: Processed {} assets, Added {} new historical records, Skipped {} existing records.",
            assets.size(),
            rowsInserted,
            skippedExistingRecords
        );
        return new HistoricalDataSyncResponse(assets.size(), rowsInserted, skippedAssets.size(), skippedAssets);
    }

    void runExtremeSyncInBackground(UUID userId) {
        List<Asset> assets = assetRepository.findAllByOrderBySymbolAsc();
        log.info("Extreme historical sync started by user {} for {} assets.", userId, assets.size());
        try {
            for (int i = 0; i < assets.size(); i++) {
                Asset asset = assets.get(i);
                boolean hasNextAsset = i < assets.size() - 1;
                log.info("Syncing {}: starting sync.", asset.getSymbol());
                try {
                    AssetSyncResult result = syncAsset(asset, null);
                    int daysFound = result.rowsInserted() + result.skippedExistingRecords();
                    if (result.assetSkippedReason() != null) {
                        log.info("Syncing {}: skipped ({})", asset.getSymbol(), result.assetSkippedReason());
                    } else if (hasNextAsset) {
                        log.info(
                            "Syncing {}: {} day found. Waiting 60 seconds before next asset...",
                            asset.getSymbol(),
                            daysFound
                        );
                    } else {
                        log.info(
                            "Syncing {}: {} day found. Final asset complete.",
                            asset.getSymbol(),
                            daysFound
                        );
                    }
                } catch (Exception ex) {
                    if (hasNextAsset) {
                        log.warn(
                            "Syncing {} failed ({}). Waiting 60 seconds before next asset...",
                            asset.getSymbol(),
                            ex.getMessage()
                        );
                    } else {
                        log.warn("Syncing {} failed ({}). Final asset complete.", asset.getSymbol(), ex.getMessage());
                    }
                } finally {
                    if (hasNextAsset && !sleepExtremeThrottle(asset.getSymbol())) {
                        break;
                    }
                }
            }
        } finally {
            extremeSyncInProgress.set(false);
            log.info("Extreme historical sync finished for user {}.", userId);
        }
    }

    private List<Asset> resolveAssetsToSync(UUID assetId) {
        if (assetId == null) {
            return assetRepository.findAllByOrderBySymbolAsc();
        }
        Asset asset = assetRepository.findById(assetId)
            .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        return List.of(asset);
    }

    @Override
    @Transactional
    public HistoricalDataSyncResponse cleanAndReset(UUID userId) {
        assetHistoricDataRepository.deleteAllInBatch();

        List<Asset> assets = assetRepository.findAllByOrderBySymbolAsc();
        int rowsInserted = 0;
        int skippedExistingRecords = 0;
        List<SkippedAssetSyncItem> skippedAssets = new ArrayList<>();
        LocalDate resetStartDate = syncCutoffDateUtc().minusDays(DEFAULT_LOOKBACK_DAYS - 1L);
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            AssetSyncResult result = syncAsset(asset, resetStartDate);
            rowsInserted += result.rowsInserted();
            skippedExistingRecords += result.skippedExistingRecords();
            if (result.assetSkippedReason() != null) {
                skippedAssets.add(new SkippedAssetSyncItem(asset.getId(), asset.getSymbol(), result.assetSkippedReason()));
            }
            sleepAfterBatchIfNeeded(i + 1, assets.size());
        }
        log.info(
            "Sync complete: Processed {} assets, Added {} new historical records, Skipped {} existing records.",
            assets.size(),
            rowsInserted,
            skippedExistingRecords
        );
        return new HistoricalDataSyncResponse(assets.size(), rowsInserted, skippedAssets.size(), skippedAssets);
    }

    private AssetSyncResult syncAsset(
        Asset asset,
        LocalDate forcedStartDate
    ) {
        String coinId = normalizeCoinGeckoId(asset.getCoinGeckoId());
        if (coinId == null) {
            return new AssetSyncResult(0, 0, "coin_gecko_id is not set");
        }

        LocalDate syncEndDate = syncCutoffDateUtc();
        LocalDate latestExistingDate = assetHistoricDataRepository.findLatestDayDateByAssetId(asset.getId());

        LocalDate startDate;
        if (forcedStartDate != null) {
            startDate = forcedStartDate;
        } else if (latestExistingDate != null) {
            startDate = latestExistingDate.plusDays(1);
        } else {
            startDate = syncEndDate.minusDays(DEFAULT_LOOKBACK_DAYS - 1L);
        }

        if (startDate.isAfter(syncEndDate)) {
            return new AssetSyncResult(0, 0, null);
        }

        Set<LocalDate> existingDates = assetHistoricDataRepository.findExistingDayDates(asset.getId(), startDate, syncEndDate);
        List<CoinGeckoClient.CoinGeckoDailyQuote> quotes;
        try {
            quotes = coinGeckoClient.fetchDailyQuotes(coinId, startDate, syncEndDate);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw ex;
            }
            return new AssetSyncResult(0, 0, ex.getReason() == null ? "failed to fetch CoinGecko data" : ex.getReason());
        }

        List<AssetHistoricData> rowsToInsert = new ArrayList<>();
        int existingRowsSkipped = 0;
        for (CoinGeckoClient.CoinGeckoDailyQuote quote : quotes) {
            if (quote.dayDate().isBefore(startDate) || quote.dayDate().isAfter(syncEndDate)) {
                continue;
            }
            if (existingDates.contains(quote.dayDate())) {
                existingRowsSkipped++;
                continue;
            }
            AssetHistoricData row = new AssetHistoricData();
            row.setAsset(asset);
            row.setDayDate(quote.dayDate());
            row.setHighPrice(quote.highPrice());
            row.setLowPrice(quote.lowPrice());
            row.setClosingPrice(quote.closePrice());
            rowsToInsert.add(row);
        }

        if (rowsToInsert.isEmpty()) {
            return new AssetSyncResult(0, existingRowsSkipped, null);
        }
        assetHistoricDataRepository.saveAll(rowsToInsert);
        return new AssetSyncResult(rowsToInsert.size(), existingRowsSkipped, null);
    }

    private LocalDate syncCutoffDateUtc() {
        return LocalDate.now(ZoneOffset.UTC).minusDays(1);
    }

    private boolean sleepExtremeThrottle(String assetSymbol) {
        try {
            Thread.sleep(EXTREME_THROTTLE_DELAY_MS);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Extreme sync sleep interrupted after {}. Stopping background job.", assetSymbol);
            return false;
        }
    }

    void sleepAfterBatchIfNeeded(int processedCount, int totalAssets) {
        int batchSize = Math.max(1, historicalSyncProperties.batchSize());
        long sleepMs = Math.max(0L, historicalSyncProperties.sleepMsBetweenBatches());
        if (sleepMs == 0L || processedCount >= totalAssets || processedCount % batchSize != 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Historical sync sleep interrupted.",
                ex
            );
        }
    }

    private HistoricalDataRowResponse toResponse(AssetHistoricData row) {
        return new HistoricalDataRowResponse(
            row.getId(),
            row.getAsset().getId(),
            row.getAsset().getSymbol(),
            row.getAsset().getName(),
            row.getDayDate(),
            row.getHighPrice(),
            row.getLowPrice(),
            row.getClosingPrice()
        );
    }

    private record AssetSyncResult(
        int rowsInserted,
        int skippedExistingRecords,
        String assetSkippedReason
    ) {
    }

    private static String normalizeCoinGeckoId(String coinGeckoId) {
        if (coinGeckoId == null || coinGeckoId.isBlank()) {
            return null;
        }
        return coinGeckoId.trim().toLowerCase();
    }
}
