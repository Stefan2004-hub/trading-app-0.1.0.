package com.trading.service.marketalert;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.AssetHistoricData;
import com.trading.domain.entity.MarketAlert;
import com.trading.domain.entity.User;
import com.trading.domain.enums.MarketAlertStrategyType;
import com.trading.domain.enums.MarketAlertType;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.AssetHistoricDataRepository;
import com.trading.domain.repository.MarketAlertRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.marketalert.MarketScanResponse;
import com.trading.dto.marketalert.MarketSnapshotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MarketAlertServiceImplTest {

    @Mock
    private MarketAlertRepository marketAlertRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AssetHistoricDataRepository assetHistoricDataRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    @InjectMocks
    private MarketAlertServiceImpl marketAlertService;

    private UUID userId;
    private UUID assetId;
    private User user;
    private Asset asset;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        user = new User();
        user.setId(userId);

        asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("BTC");
        asset.setName("Bitcoin");

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(transactionRepository.findDistinctInvestedAssetsByUserId(userId)).thenReturn(List.of(asset));
        lenient().when(assetRepository.findAllByOrderBySymbolAsc()).thenReturn(List.of(asset));
        lenient().when(cacheManager.getCache("marketTechnicalSummary")).thenReturn(cache);
        lenient().when(marketAlertRepository.save(any(MarketAlert.class))).thenAnswer(invocation -> {
            MarketAlert alert = invocation.getArgument(0, MarketAlert.class);
            alert.setId(UUID.randomUUID());
            return alert;
        });
    }

    @Test
    void scanCreatesFixedAndDynamicBuyAlertsWhenSignalMatches() {
        List<AssetHistoricData> rows = buildBuySignalRows();
        when(assetHistoricDataRepository.findAllByAsset_IdOrderByDayDateAsc(assetId)).thenReturn(rows);
        when(marketAlertRepository.existsByUser_IdAndAsset_IdAndAlertTypeAndStrategyTypeAndTriggerDate(
            userId,
            assetId,
            MarketAlertType.BUY,
            MarketAlertStrategyType.FIXED_14D,
            rows.get(rows.size() - 1).getDayDate()
        )).thenReturn(false);
        when(marketAlertRepository.existsByUser_IdAndAsset_IdAndAlertTypeAndStrategyTypeAndTriggerDate(
            userId,
            assetId,
            MarketAlertType.BUY,
            MarketAlertStrategyType.DYNAMIC,
            rows.get(rows.size() - 1).getDayDate()
        )).thenReturn(false);

        MarketScanResponse response = marketAlertService.scan(userId);

        assertEquals(1, response.assetsProcessed());
        assertEquals(2, response.alertsCreated());
        assertEquals(1, response.fixedAlertsCreated());
        assertEquals(1, response.dynamicAlertsCreated());
        assertEquals(1, response.snapshotsUpdated());
        verify(assetHistoricDataRepository).findAllByAsset_IdOrderByDayDateAsc(assetId);
        verify(cache).put(any(), any());
        verify(marketAlertRepository, times(2)).save(any(MarketAlert.class));
    }

    @Test
    void scanSkipsDuplicatesForSameAssetTypeStrategyAndDate() {
        List<AssetHistoricData> rows = buildBuySignalRows();
        LocalDate triggerDate = rows.get(rows.size() - 1).getDayDate();

        when(assetHistoricDataRepository.findAllByAsset_IdOrderByDayDateAsc(assetId)).thenReturn(rows);
        when(marketAlertRepository.existsByUser_IdAndAsset_IdAndAlertTypeAndStrategyTypeAndTriggerDate(
            userId,
            assetId,
            MarketAlertType.BUY,
            MarketAlertStrategyType.FIXED_14D,
            triggerDate
        )).thenReturn(true);
        when(marketAlertRepository.existsByUser_IdAndAsset_IdAndAlertTypeAndStrategyTypeAndTriggerDate(
            userId,
            assetId,
            MarketAlertType.BUY,
            MarketAlertStrategyType.DYNAMIC,
            triggerDate
        )).thenReturn(true);

        MarketScanResponse response = marketAlertService.scan(userId);

        assertEquals(0, response.alertsCreated());
        assertEquals(0, response.fixedAlertsCreated());
        assertEquals(0, response.dynamicAlertsCreated());
        assertEquals(1, response.snapshotsUpdated());
        verify(marketAlertRepository, never()).save(any(MarketAlert.class));
    }

    @Test
    void listTechnicalSummaryReturnsEmptyWhenCacheIsMissing() {
        when(cacheManager.getCache("marketTechnicalSummary")).thenReturn(null);

        List<MarketSnapshotDTO> summary = marketAlertService.listTechnicalSummary();

        assertTrue(summary.isEmpty());
    }

    @Test
    void listTechnicalSummaryReturnsCachedSnapshots() {
        List<MarketSnapshotDTO> cached = List.of(
            new MarketSnapshotDTO("Bitcoin", "BTC", new BigDecimal("30.0000"), new BigDecimal("20.0000"), LocalDate.of(2026, 2, 22), "UP")
        );
        Cache.ValueWrapper wrapper = () -> cached;
        when(cache.get("GLOBAL")).thenReturn(wrapper);

        List<MarketSnapshotDTO> summary = marketAlertService.listTechnicalSummary();

        assertEquals(1, summary.size());
        assertEquals("BTC", summary.get(0).symbol());
    }

    @Test
    void scanStoresMomentumAsUpWhenCurrentRsiIsHigherThanYesterday() {
        when(assetHistoricDataRepository.findAllByAsset_IdOrderByDayDateAsc(assetId))
            .thenReturn(buildRowsWithCloses(List.of(
                100, 99, 98, 97, 96, 95, 94, 93, 92, 91, 90, 89, 88, 87, 120
            )));

        marketAlertService.scan(userId);

        ArgumentCaptor<List<MarketSnapshotDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(cache).put(any(), captor.capture());
        assertEquals("UP", captor.getValue().get(0).momentum());
    }

    @Test
    void scanStoresMomentumAsDownWhenCurrentRsiIsLowerThanYesterday() {
        when(assetHistoricDataRepository.findAllByAsset_IdOrderByDayDateAsc(assetId))
            .thenReturn(buildRowsWithCloses(List.of(
                100, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 90
            )));

        marketAlertService.scan(userId);

        ArgumentCaptor<List<MarketSnapshotDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(cache).put(any(), captor.capture());
        assertEquals("DOWN", captor.getValue().get(0).momentum());
    }

    @Test
    void scanStoresMomentumAsFlatWhenCurrentRsiMatchesYesterday() {
        when(assetHistoricDataRepository.findAllByAsset_IdOrderByDayDateAsc(assetId))
            .thenReturn(buildRowsWithCloses(List.of(
                100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100
            )));

        marketAlertService.scan(userId);

        ArgumentCaptor<List<MarketSnapshotDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(cache).put(any(), captor.capture());
        assertEquals("FLAT", captor.getValue().get(0).momentum());
    }

    @Test
    void calculateRSIReturnsHundredWhenAverageLossIsZero() {
        List<AssetHistoricData> rows = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2026, 1, 1);

        for (int i = 0; i < 14; i++) {
            BigDecimal close = new BigDecimal(100 + i);
            rows.add(row(startDate.plusDays(i), close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close));
        }

        BigDecimal rsi = MarketAlertServiceImpl.calculateRSI(14, rows);

        assertTrue(rsi.compareTo(new BigDecimal("100")) == 0);
    }

    private static List<AssetHistoricData> buildBuySignalRows() {
        List<AssetHistoricData> rows = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        List<BigDecimal> closes = List.of(
            new BigDecimal("100"),
            new BigDecimal("101"),
            new BigDecimal("97"),
            new BigDecimal("94"),
            new BigDecimal("91"),
            new BigDecimal("88"),
            new BigDecimal("85"),
            new BigDecimal("82"),
            new BigDecimal("79"),
            new BigDecimal("76"),
            new BigDecimal("73"),
            new BigDecimal("70"),
            new BigDecimal("67"),
            new BigDecimal("64")
        );

        for (int i = 0; i < closes.size(); i++) {
            BigDecimal close = closes.get(i);
            rows.add(row(startDate.plusDays(i), close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close));
        }
        return rows;
    }

    private static AssetHistoricData row(LocalDate date, BigDecimal high, BigDecimal low, BigDecimal close) {
        AssetHistoricData row = new AssetHistoricData();
        row.setDayDate(date);
        row.setHighPrice(high);
        row.setLowPrice(low);
        row.setClosingPrice(close);
        row.setCreatedAt(OffsetDateTime.now());
        return row;
    }

    private static List<AssetHistoricData> buildRowsWithCloses(List<Integer> closes) {
        List<AssetHistoricData> rows = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < closes.size(); i++) {
            BigDecimal close = new BigDecimal(closes.get(i));
            rows.add(row(startDate.plusDays(i), close.add(BigDecimal.ONE), close.subtract(BigDecimal.ONE), close));
        }
        return rows;
    }
}
