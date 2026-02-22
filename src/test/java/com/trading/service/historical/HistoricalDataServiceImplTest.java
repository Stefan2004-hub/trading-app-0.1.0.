package com.trading.service.historical;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.dto.historical.SkippedAssetSyncItem;
import com.trading.service.historical.coingecko.CoinGeckoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricalDataServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AssetHistoricDataRepository assetHistoricDataRepository;
    @Mock
    private CoinGeckoClient coinGeckoClient;

    @InjectMocks
    private HistoricalDataServiceImpl historicalDataService;

    private UUID userId;
    private Asset btcAsset;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        btcAsset = new Asset();
        btcAsset.setId(UUID.randomUUID());
        btcAsset.setSymbol("BTC");
        btcAsset.setName("Bitcoin");
        btcAsset.setCoinGeckoId("bitcoin");
    }

    @Test
    void syncIncrementalDeletesTodayRowsBeforeSync() {
        when(transactionRepository.findDistinctInvestedAssetsByUserId(userId)).thenReturn(List.of());

        historicalDataService.syncIncremental(userId);

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        var inOrder = inOrder(assetHistoricDataRepository, transactionRepository);
        inOrder.verify(assetHistoricDataRepository).deleteByDayDate(todayUtc);
        inOrder.verify(transactionRepository).findDistinctInvestedAssetsByUserId(userId);
    }

    @Test
    void syncIncrementalDeletesTodayAndThenSavesNewQuotes() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate latestExisting = todayUtc.minusDays(2);
        LocalDate expectedStart = latestExisting.plusDays(1);

        when(transactionRepository.findDistinctInvestedAssetsByUserId(userId)).thenReturn(List.of(btcAsset));
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

        HistoricalDataSyncResponse response = historicalDataService.syncIncremental(userId);

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

        when(transactionRepository.findDistinctInvestedAssetsByUserId(userId)).thenReturn(List.of(btcAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(btcAsset.getId())).thenReturn(todayUtc.minusDays(1));
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), todayUtc, todayUtc)).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes("bitcoin", todayUtc, todayUtc))
            .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "CoinGecko rate limit reached."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> historicalDataService.syncIncremental(userId));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getStatusCode().value());
        verify(assetHistoricDataRepository).deleteByDayDate(todayUtc);
        verify(assetHistoricDataRepository, never()).saveAll(any());
    }

    @Test
    void syncIncrementalNon429ErrorSkipsAssetAndKeepsFlow() {
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);

        when(transactionRepository.findDistinctInvestedAssetsByUserId(userId)).thenReturn(List.of(btcAsset));
        when(assetHistoricDataRepository.findLatestDayDateByAssetId(btcAsset.getId())).thenReturn(todayUtc.minusDays(1));
        when(assetHistoricDataRepository.findExistingDayDates(btcAsset.getId(), todayUtc, todayUtc)).thenReturn(Set.of());
        when(coinGeckoClient.fetchDailyQuotes(eq("bitcoin"), eq(todayUtc), eq(todayUtc)))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "upstream failed"));

        HistoricalDataSyncResponse response = historicalDataService.syncIncremental(userId);

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
}
