package com.trading.service.historical;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.dto.historical.SkippedAssetSyncItem;
import com.trading.service.historical.coingecko.CoinGeckoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceImplTest {

    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetHistoricDataRepository assetHistoricDataRepository;
    @Mock
    private CoinGeckoClient coinGeckoClient;
    private HistoricalSyncProperties historicalSyncProperties;

    private HistoricalDataServiceImpl historicalDataService;

    private UUID userId;
    private Asset btcAsset;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        historicalSyncProperties = new HistoricalSyncProperties(50, 0);
        historicalDataService = new HistoricalDataServiceImpl(
            assetRepository,
            assetHistoricDataRepository,
            coinGeckoClient,
            historicalSyncProperties
        );
        btcAsset = new Asset();
        btcAsset.setId(UUID.randomUUID());
        btcAsset.setSymbol("BTC");
        btcAsset.setName("Bitcoin");
        btcAsset.setCoinGeckoId("bitcoin");
    }

    @Test
    void syncIncrementalDeletesTodayRowsBeforeSync() {
        when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of());

        historicalDataService.syncIncremental(userId, null);

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        var inOrder = inOrder(assetHistoricDataRepository, assetRepository);
        inOrder.verify(assetHistoricDataRepository).deleteByDayDate(todayUtc);
        inOrder.verify(assetRepository).findAllByOrderBySymbolAsc();
    }

    @Test
    void syncIncrementalDeletesTodayAndThenSavesNewQuotes() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate latestExisting = todayUtc.minusDays(2);
        LocalDate expectedStart = latestExisting.plusDays(1);

        when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of(btcAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(btcAsset.getId())).thenReturn(latestExisting);
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), expectedStart, todayUtc)).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes("bitcoin", expectedStart, todayUtc)).thenReturn(List.of(
            new CoinGeckoClient.CoinGeckoDailyQuote(
                expectedStart,
                new java.math.BigDecimal("68000.00"),
                new java.math.BigDecimal("65000.00"),
                new java.math.BigDecimal("67000.00")
            )
        ));

        HistoricalDataSyncResponse response = historicalDataService.syncIncremental(userId, null);

        assertEquals(1, response.assetsProcessed());
        assertEquals(1, response.rowsInserted());
        assertEquals(0, response.assetsSkipped());
        verify(assetHistoricDataRepository).deleteByDayDate(todayUtc);

        ArgumentCaptor<List<AssetHistoricData>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(assetHistoricDataRepository).saveAll(rowsCaptor.capture());
        List<AssetHistoricData> savedRows = rowsCaptor.getValue();
        assertEquals(1, savedRows.size());
        assertEquals(expectedStart, savedRows.get(0).getDayDate());
        assertEquals(btcAsset.getId(), savedRows.get(0).getAsset().getId());
    }

    @Test
    void syncIncrementalRateLimitStillPropagates() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of(btcAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(btcAsset.getId())).thenReturn(todayUtc.minusDays(1));
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), todayUtc, todayUtc)).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes("bitcoin", todayUtc, todayUtc))
            .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "CoinGecko rate limit reached."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historicalDataService.syncIncremental(userId, null));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getStatusCode().value());
        verify(assetHistoricDataRepository).deleteByDayDate(todayUtc);
        verify(assetHistoricDataRepository, never()).saveAll(any());
    }

    @Test
    void syncIncrementalNon429ErrorSkipsAssetAndKeepsFlow() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of(btcAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(btcAsset.getId())).thenReturn(todayUtc.minusDays(1));
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), todayUtc, todayUtc)).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes(eq("bitcoin"), eq(todayUtc), eq(todayUtc)))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream failed"));

        HistoricalDataSyncResponse response = historicalDataService.syncIncremental(userId, null);

        assertEquals(1, response.assetsProcessed());
        assertEquals(0, response.rowsInserted());
        assertEquals(1, response.assetsSkipped());
        assertEquals(1, response.skippedAssets().size());
        SkippedAssetSyncItem skipped = response.skippedAssets().get(0);
        assertEquals(btcAsset.getId(), skipped.assetId());
        assertEquals("BTC", skipped.assetSymbol());
        assertEquals("upstream failed", skipped.reason());
        verify(assetHistoricDataRepository).deleteByDayDate(todayUtc);
    }

    @Test
    void cleanAndResetDeletesAllThenFetchesAllAssetsFromThirtyDayLookback() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate resetStartDate = todayUtc.minusDays(29);
        when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of(btcAsset));
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), resetStartDate, todayUtc)).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes("bitcoin", resetStartDate, todayUtc)).thenReturn(List.of());

        HistoricalDataSyncResponse response = historicalDataService.cleanAndReset(userId);

        assertEquals(1, response.assetsProcessed());
        assertEquals(0, response.rowsInserted());
        verify(assetHistoricDataRepository).deleteAllInBatch();
        verify(coinGeckoClient).fetchDailyQuotes("bitcoin", resetStartDate, todayUtc);
    }

    @Test
    void syncIncrementalSkipsExistingRowsAndStillInsertsNewRows() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = todayUtc.minusDays(1);
        LocalDate existingDate = startDate;
        LocalDate newDate = todayUtc;

        when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of(btcAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(btcAsset.getId())).thenReturn(startDate.minusDays(1));
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), startDate, todayUtc)).thenReturn(Set.of(existingDate));
        when(coinGeckoClient.fetchDailyQuotes("bitcoin", startDate, todayUtc)).thenReturn(List.of(
            new CoinGeckoClient.CoinGeckoDailyQuote(existingDate, new java.math.BigDecimal("100"), new java.math.BigDecimal("90"), new java.math.BigDecimal("95")),
            new CoinGeckoClient.CoinGeckoDailyQuote(newDate, new java.math.BigDecimal("110"), new java.math.BigDecimal("100"), new java.math.BigDecimal("105"))
        ));

        HistoricalDataSyncResponse response = historicalDataService.syncIncremental(userId, null);

        assertEquals(1, response.rowsInserted());
        verify(assetHistoricDataRepository).saveAll(any());
    }

    @Test
    void syncIncrementalInvokesBatchSleepHookBetweenBatches() {
        HistoricalDataServiceImpl serviceWithSpySleep = spy(new HistoricalDataServiceImpl(
            assetRepository,
            assetHistoricDataRepository,
            coinGeckoClient,
            new HistoricalSyncProperties(1, 1000)
        ));
        doNothing().when(serviceWithSpySleep).sleepAfterBatchIfNeeded(anyInt(), anyInt());
        Asset secondAsset = new Asset();
        secondAsset.setId(UUID.randomUUID());
        secondAsset.setSymbol("ETH");
        secondAsset.setName("Ethereum");
        secondAsset.setCoinGeckoId("ethereum");

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of(btcAsset, secondAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(any())).thenReturn(todayUtc.minusDays(1));
        when(assetHistoricDataRepository.findExistingDayDates(any(), eq(todayUtc), eq(todayUtc))).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes(any(), any(), any())).thenReturn(List.of());

        serviceWithSpySleep.syncIncremental(userId, null);

        verify(serviceWithSpySleep).sleepAfterBatchIfNeeded(1, 2);
        verify(serviceWithSpySleep).sleepAfterBatchIfNeeded(2, 2);
    }

    @Test
    void syncIncrementalWithSpecificAssetRefreshesOnlySelectedAsset() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate latestExisting = todayUtc.minusDays(1);
        when(assetRepository.findById(btcAsset.getId())).thenReturn(Optional.of(btcAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(btcAsset.getId())).thenReturn(latestExisting);
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), todayUtc, todayUtc)).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes("bitcoin", todayUtc, todayUtc)).thenReturn(List.of());

        HistoricalDataSyncResponse response = historicalDataService.syncIncremental(userId, btcAsset.getId());

        assertEquals(1, response.assetsProcessed());
        verify(assetRepository).findById(btcAsset.getId());
        verify(assetRepository, never()).findAllByOrderBySymbolAsc();
    }

    @Test
    void syncIncrementalWithUnknownAssetThrows() {
        UUID unknownAssetId = UUID.randomUUID();
        when(assetRepository.findById(unknownAssetId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> historicalDataService.syncIncremental(userId, unknownAssetId)
        );

        assertEquals("Asset not found: " + unknownAssetId, ex.getMessage());
        verify(assetHistoricDataRepository, never()).saveAll(any());
    }

    @Test
    void listAssetsNeedingRefreshTodayReturnsMissingAssetsForTodayUtc() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        when(assetRepository.findAssetsMissingHistoricalDataForDay(todayUtc)).thenReturn(List.of(btcAsset));

        var response = historicalDataService.listAssetsNeedingRefreshToday(userId);

        assertEquals(1, response.size());
        assertEquals(btcAsset.getId(), response.get(0).assetId());
        assertEquals("BTC", response.get(0).assetSymbol());
        assertEquals("Bitcoin", response.get(0).assetName());
        assertEquals(todayUtc, response.get(0).missingDate());
    }
}
