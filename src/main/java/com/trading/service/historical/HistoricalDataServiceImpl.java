package com.trading.service.historical;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.historical.enabled", havingValue = "true", matchIfMissing = true)
public class HistoricalDataServiceImpl implements HistoricalDataService {

    private static final Logger log = LoggerFactory.getLogger(HistoricalDataServiceImpl.class);
    private static final int DEFAULT_LOOKBACK_DAYS = 30;

    private final AssetRepository assetRepository;
    private final AssetHistoricDataRepository assetHistoricDataRepository;
    private final CoinGeckoClient coinGeckoClient;
    private final HistoricalSyncProperties historicalSyncProperties;

    public HistoricalDataServiceImpl(
        AssetRepository assetRepository,
        AssetHistoricDataRepository assetHistoricDataRepository,
        CoinGeckoClient coinGeckoClient,
        HistoricalSyncProperties historicalSyncProperties
    ) {
        this.assetRepository = assetRepository;
        this.assetHistoricDataRepository = assetHistoricDataRepository;
        this.coinGeckoClient = coinGeckoClient;
        this.historicalSyncProperties = historicalSyncProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoricalDataRowResponse> listForUser(UUID userId, int page, int size) {
        return assetHistoricDataRepository.findAllForUserInvestedAssets(userId, PageRequest.of(page, size))
            .map(this::toResponse);
    }

    @Override
    @Transactional
    public HistoricalDataSyncResponse syncIncremental(UUID userId, UUID assetId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        assetHistoricDataRepository.deleteByDayDate(today);

        List<Asset> assets = resolveAssetsToSync(assetId);
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
        LocalDate resetStartDate = LocalDate.now(ZoneOffset.UTC).minusDays(DEFAULT_LOOKBACK_DAYS - 1L);
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

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate latestExistingDate = assetHistoricDataRepository.findLatestDayDateByAssetId(asset.getId());

        LocalDate startDate;
        if (forcedStartDate != null) {
            startDate = forcedStartDate;
        } else if (latestExistingDate != null) {
            startDate = latestExistingDate.plusDays(1);
        } else {
            startDate = today.minusDays(DEFAULT_LOOKBACK_DAYS - 1L);
        }

        if (startDate.isAfter(today)) {
            return new AssetSyncResult(0, 0, null);
        }

        Set<LocalDate> existingDates = assetHistoricDataRepository.findExistingDayDates(asset.getId(), startDate, today);
        List<CoinGeckoClient.CoinGeckoDailyQuote> quotes;
        try {
            quotes = coinGeckoClient.fetchDailyQuotes(coinId, startDate, today);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw ex;
            }
            return new AssetSyncResult(0, 0, ex.getReason() == null ? "failed to fetch CoinGecko data" : ex.getReason());
        }

        List<AssetHistoricData> rowsToInsert = new ArrayList<>();
        int existingRowsSkipped = 0;
        for (CoinGeckoClient.CoinGeckoDailyQuote quote : quotes) {
            if (quote.dayDate().isBefore(startDate) || quote.dayDate().isAfter(today)) {
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
