package com.trading.service.strategy;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.BuyStrategy;
import com.trading.domain.entity.User;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.BuyStrategyRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.strategy.BuyStrategyResponse;
import com.trading.dto.strategy.UpsertBuyStrategyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuyStrategyServiceImplTest {

    @Mock
    private BuyStrategyRepository buyStrategyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private BuyStrategyServiceImpl buyStrategyService;

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

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        lenient().when(buyStrategyRepository.save(any(BuyStrategy.class))).thenAnswer(invocation -> {
            BuyStrategy strategy = invocation.getArgument(0, BuyStrategy.class);
            if (strategy.getId() == null) {
                strategy.setId(UUID.randomUUID());
            }
            return strategy;
        });
    }

    @Test
    void upsertCreatesStrategyWhenMissing() {
        when(buyStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId))
            .thenReturn(Optional.empty());

        BuyStrategyResponse response = buyStrategyService.upsert(
            userId,
            new UpsertBuyStrategyRequest(assetId, new BigDecimal("8.50"), new BigDecimal("250.00"), null)
        );

        assertEquals(userId, response.userId());
        assertEquals(assetId, response.assetId());
        assertEquals(0, response.dipThresholdPercent().compareTo(new BigDecimal("8.50")));
        assertEquals(0, response.buyAmountUsd().compareTo(new BigDecimal("250.00")));
        assertEquals(Boolean.TRUE, response.active());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void upsertUpdatesExistingStrategyForSameAsset() {
        BuyStrategy existing = new BuyStrategy();
        existing.setId(UUID.randomUUID());
        existing.setUser(user);
        existing.setAsset(asset);
        existing.setDipThresholdPercent(new BigDecimal("5.00"));
        existing.setBuyAmountUsd(new BigDecimal("100.00"));
        existing.setActive(Boolean.TRUE);
        existing.setCreatedAt(java.time.OffsetDateTime.parse("2026-02-10T10:00:00Z"));
        existing.setUpdatedAt(java.time.OffsetDateTime.parse("2026-02-10T10:00:00Z"));

        when(buyStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId))
            .thenReturn(Optional.of(existing));

        BuyStrategyResponse response = buyStrategyService.upsert(
            userId,
            new UpsertBuyStrategyRequest(assetId, new BigDecimal("11.00"), new BigDecimal("300.00"), Boolean.FALSE)
        );

        assertEquals(existing.getId(), response.id());
        assertEquals(0, response.dipThresholdPercent().compareTo(new BigDecimal("11.00")));
        assertEquals(0, response.buyAmountUsd().compareTo(new BigDecimal("300.00")));
        assertEquals(Boolean.FALSE, response.active());
        assertEquals(existing.getCreatedAt(), response.createdAt());
    }

    @Test
    void upsertRejectsNonPositiveThreshold() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> buyStrategyService.upsert(
                userId,
                new UpsertBuyStrategyRequest(assetId, BigDecimal.ZERO, new BigDecimal("200.00"), Boolean.TRUE)
            )
        );

        assertEquals("dipThresholdPercent must be positive", ex.getMessage());
    }

    @Test
    void upsertRejectsNonPositiveBuyAmount() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> buyStrategyService.upsert(
                userId,
                new UpsertBuyStrategyRequest(assetId, new BigDecimal("5.00"), BigDecimal.ZERO, Boolean.TRUE)
            )
        );

        assertEquals("buyAmountUsd must be positive", ex.getMessage());
    }
}
