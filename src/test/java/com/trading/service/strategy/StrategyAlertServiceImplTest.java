package com.trading.service.strategy;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.BuyStrategy;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.SellStrategy;
import com.trading.domain.entity.StrategyAlert;
import com.trading.domain.entity.Transaction;
import com.trading.domain.entity.User;
import com.trading.domain.enums.StrategyAlertStatus;
import com.trading.domain.enums.StrategyType;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.BuyStrategyRepository;
import com.trading.domain.repository.PricePeakRepository;
import com.trading.domain.repository.SellStrategyRepository;
import com.trading.domain.repository.StrategyAlertRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.strategy.GenerateStrategyAlertsRequest;
import com.trading.dto.strategy.StrategyAlertResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StrategyAlertServiceImplTest {

    @Mock
    private StrategyAlertRepository strategyAlertRepository;
    @Mock
    private SellStrategyRepository sellStrategyRepository;
    @Mock
    private BuyStrategyRepository buyStrategyRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private PricePeakRepository pricePeakRepository;

    @InjectMocks
    private StrategyAlertServiceImpl strategyAlertService;

    private UUID userId;
    private UUID assetId;
    private User user;
    private Asset asset;
    private SellStrategy sellStrategy;
    private BuyStrategy buyStrategy;
    private Transaction latestBuyTx;
    private PricePeak pricePeak;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        assetId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        asset = new Asset();
        asset.setId(assetId);
        asset.setSymbol("BTC");

        sellStrategy = new SellStrategy();
        sellStrategy.setId(UUID.randomUUID());
        sellStrategy.setUser(user);
        sellStrategy.setAsset(asset);
        sellStrategy.setThresholdPercent(new BigDecimal("10.00"));
        sellStrategy.setActive(Boolean.TRUE);

        buyStrategy = new BuyStrategy();
        buyStrategy.setId(UUID.randomUUID());
        buyStrategy.setUser(user);
        buyStrategy.setAsset(asset);
        buyStrategy.setDipThresholdPercent(new BigDecimal("10.00"));
        buyStrategy.setBuyAmountUsd(new BigDecimal("200.00"));
        buyStrategy.setActive(Boolean.TRUE);

        latestBuyTx = new Transaction();
        latestBuyTx.setId(UUID.randomUUID());
        latestBuyTx.setUser(user);
        latestBuyTx.setAsset(asset);
        latestBuyTx.setTransactionType(TransactionType.BUY);
        latestBuyTx.setUnitPriceUsd(new BigDecimal("100.00"));
        latestBuyTx.setNetAmount(new BigDecimal("1.00"));

        pricePeak = new PricePeak();
        pricePeak.setId(UUID.randomUUID());
        pricePeak.setUser(user);
        pricePeak.setAsset(asset);
        pricePeak.setPeakPrice(new BigDecimal("120.00"));
        pricePeak.setActive(Boolean.TRUE);

        lenient().when(sellStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId))
            .thenReturn(Optional.of(sellStrategy));
        lenient().when(buyStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId))
            .thenReturn(Optional.of(buyStrategy));
        lenient().when(transactionRepository.findAllByUser_IdAndAsset_IdAndTransactionTypeOrderByTransactionDateDesc(
            userId,
            assetId,
            TransactionType.BUY
        )).thenReturn(List.of(latestBuyTx));
        lenient().when(pricePeakRepository.findByUser_IdAndAsset_IdAndActiveTrue(userId, assetId))
            .thenReturn(Optional.of(pricePeak));
        lenient().when(strategyAlertRepository.save(any(StrategyAlert.class))).thenAnswer(invocation -> {
            StrategyAlert alert = invocation.getArgument(0, StrategyAlert.class);
            alert.setId(UUID.randomUUID());
            return alert;
        });
    }

    @Test
    void generateCreatesSellAlertWhenThresholdIsReached() {
        lenient().when(strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId, assetId, StrategyType.SELL, StrategyAlertStatus.PENDING
        )).thenReturn(false);
        lenient().when(strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId, assetId, StrategyType.BUY, StrategyAlertStatus.PENDING
        )).thenReturn(true);

        List<StrategyAlertResponse> generated = strategyAlertService.generate(
            userId,
            new GenerateStrategyAlertsRequest(assetId, new BigDecimal("111.00"))
        );

        assertEquals(1, generated.size());
        assertEquals(StrategyType.SELL, generated.get(0).strategyType());
        assertEquals(StrategyAlertStatus.PENDING, generated.get(0).status());
        assertEquals(0, generated.get(0).triggerPrice().compareTo(new BigDecimal("111.00")));
    }

    @Test
    void generateCreatesBuyAlertWhenDipIsReached() {
        lenient().when(strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId, assetId, StrategyType.SELL, StrategyAlertStatus.PENDING
        )).thenReturn(true);
        lenient().when(strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId, assetId, StrategyType.BUY, StrategyAlertStatus.PENDING
        )).thenReturn(false);

        List<StrategyAlertResponse> generated = strategyAlertService.generate(
            userId,
            new GenerateStrategyAlertsRequest(assetId, new BigDecimal("108.00"))
        );

        assertEquals(1, generated.size());
        assertEquals(StrategyType.BUY, generated.get(0).strategyType());
        assertEquals(StrategyAlertStatus.PENDING, generated.get(0).status());
        assertEquals(0, generated.get(0).triggerPrice().compareTo(new BigDecimal("108.00")));
    }

    @Test
    void generateSkipsWhenPendingAlertAlreadyExists() {
        lenient().when(strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId, assetId, StrategyType.SELL, StrategyAlertStatus.PENDING
        )).thenReturn(true);
        lenient().when(strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId, assetId, StrategyType.BUY, StrategyAlertStatus.PENDING
        )).thenReturn(true);

        List<StrategyAlertResponse> generated = strategyAlertService.generate(
            userId,
            new GenerateStrategyAlertsRequest(assetId, new BigDecimal("50.00"))
        );

        assertTrue(generated.isEmpty());
    }

    @Test
    void acknowledgeUpdatesStatusAndTimestamp() {
        StrategyAlert pending = new StrategyAlert();
        pending.setId(UUID.randomUUID());
        pending.setUser(user);
        pending.setAsset(asset);
        pending.setStrategyType(StrategyType.SELL);
        pending.setTriggerPrice(new BigDecimal("110.00"));
        pending.setThresholdPercent(new BigDecimal("10.00"));
        pending.setReferencePrice(new BigDecimal("100.00"));
        pending.setAlertMessage("Sell threshold reached for BTC");
        pending.setStatus(StrategyAlertStatus.PENDING);
        pending.setCreatedAt(OffsetDateTime.parse("2026-02-13T09:00:00Z"));

        when(strategyAlertRepository.findByIdAndUser_Id(pending.getId(), userId)).thenReturn(Optional.of(pending));
        when(strategyAlertRepository.save(any(StrategyAlert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StrategyAlertResponse response = strategyAlertService.acknowledge(userId, pending.getId());

        assertEquals(StrategyAlertStatus.ACKNOWLEDGED, response.status());
        assertNotNull(response.acknowledgedAt());
    }
}
