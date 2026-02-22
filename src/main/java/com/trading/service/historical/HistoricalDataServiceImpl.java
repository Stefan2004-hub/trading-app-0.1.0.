package com.trading.service.historical;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.dto.historical.SkippedAssetSyncItem;
import com.trading.service.historical.coingecko.CoinGeckoAssetResolver;
import com.trading.service.historical.coingecko.CoinGeckoClient;
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

@Service
public class HistoricalDataServiceImpl implements HistoricalDataService {

    private static final int DEFAULT_LOOKBACK_DAYS = 30;

    private final TransactionRepository transactionRepository;
    private final AssetHistoricDataRepository assetHistoricDataRepository;
    private final CoinGeckoClient coinGeckoClient;
    private final CoinGeckoAssetResolver coinGeckoAssetResolver;

    public HistoricalDataServiceImpl(
        TransactionRepository transactionRepository,
        AssetHistoricDataRepository assetHistoricDataRepository,
        CoinGeckoClient coinGeckoClient,
        CoinGeckoAssetResolver coinGeckoAssetResolver
    ) {
        this.transactionRepository = transactionRepository;
        this.assetHistoricDataRepository = assetHistoricDataRepository;
        this.coinGeckoClient = coinGeckoClient;
        this.coinGeckoAssetResolver = coinGeckoAssetResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalDataRowResponse> listForUser(UUID userId) {
        return assetHistoricDataRepository.findAllForUserInvestedAssets(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public HistoricalDataSyncResponse syncIncremental(UUID userId) {
        List<Asset> investedAssets = transactionRepository.findDistinctInvestedAssetsByUserId(userId);
        int rowsInserted = 0;
        List<SkippedAssetSyncItem> skippedAssets = new ArrayList<>();
        for (Asset asset : investedAssets) {
            AssetSyncResult result = syncAsset(userId, asset, null);
            rowsInserted += result.rowsInserted();
            if (result.skippedReason() != null) {
                skippedAssets.add(new SkippedAssetSyncItem(asset.getId(), asset.getSymbol(), result.skippedReason()));
            }
        }
        return new HistoricalDataSyncResponse(investedAssets.size(), rowsInserted, skippedAssets.size(), skippedAssets);
    }

    @Override
    @Transactional
    public HistoricalDataSyncResponse cleanAndReset(UUID userId) {
        assetHistoricDataRepository.deleteAllInBatch();

        List<Asset> investedAssets = transactionRepository.findDistinctInvestedAssetsByUserId(userId);
        int rowsInserted = 0;
        List<SkippedAssetSyncItem> skippedAssets = new ArrayList<>();
        LocalDate resetStartDate = LocalDate.now(ZoneOffset.UTC).minusDays(DEFAULT_LOOKBACK_DAYS - 1L);
        for (Asset asset : investedAssets) {
            AssetSyncResult result = syncAsset(userId, asset, resetStartDate);
            rowsInserted += result.rowsInserted();
            if (result.skippedReason() != null) {
                skippedAssets.add(new SkippedAssetSyncItem(asset.getId(), asset.getSymbol(), result.skippedReason()));
            }
        }
        return new HistoricalDataSyncResponse(investedAssets.size(), rowsInserted, skippedAssets.size(), skippedAssets);
    }

    private AssetSyncResult syncAsset(
        UUID userId,
        Asset asset,
        LocalDate forcedStartDate
    ) {
        final String coinId;
        try {
            coinId = coinGeckoAssetResolver.resolveCoinId(asset.getSymbol(), asset.getName());
        } catch (IllegalArgumentException ex) {
            return new AssetSyncResult(0, ex.getMessage());
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate latestExistingDate = assetHistoricDataRepository.findLatestDayDateByAssetId(asset.getId());

        LocalDate startDate;
        if (forcedStartDate != null) {
            startDate = forcedStartDate;
        } else if (latestExistingDate != null) {
            startDate = latestExistingDate.plusDays(1);
        } else {
            startDate = resolveFirstSyncDate(userId, asset.getId(), today);
        }

        if (startDate.isAfter(today)) {
            return new AssetSyncResult(0, null);
        }

        Set<LocalDate> existingDates = assetHistoricDataRepository.findExistingDayDates(asset.getId(), startDate, today);
        List<CoinGeckoClient.CoinGeckoDailyQuote> quotes;
        try {
            quotes = coinGeckoClient.fetchDailyQuotes(coinId, startDate, today);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw ex;
            }
            return new AssetSyncResult(0, ex.getReason() == null ? "failed to fetch CoinGecko data" : ex.getReason());
        }

        List<AssetHistoricData> rowsToInsert = new ArrayList<>();
        for (CoinGeckoClient.CoinGeckoDailyQuote quote : quotes) {
            if (quote.dayDate().isBefore(startDate) || quote.dayDate().isAfter(today) || existingDates.contains(quote.dayDate())) {
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
            return new AssetSyncResult(0, null);
        }
        assetHistoricDataRepository.saveAll(rowsToInsert);
        return new AssetSyncResult(rowsToInsert.size(), null);
    }

    private LocalDate resolveFirstSyncDate(UUID userId, UUID assetId, LocalDate today) {
        OffsetDateTime firstBuyDate = transactionRepository.findEarliestBuyTransactionDate(userId, assetId);
        if (firstBuyDate == null) {
            return today.minusDays(DEFAULT_LOOKBACK_DAYS - 1L);
        }
        LocalDate date = firstBuyDate.toLocalDate();
        LocalDate maxLookbackDate = today.minusDays(DEFAULT_LOOKBACK_DAYS - 1L);
        return date.isBefore(maxLookbackDate) ? maxLookbackDate : date;
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
        String skippedReason
    ) {
    }
}
