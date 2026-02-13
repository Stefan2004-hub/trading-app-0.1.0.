package com.trading.service.strategy;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.SellStrategy;
import com.trading.domain.entity.User;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.SellStrategyRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.strategy.SellStrategyResponse;
import com.trading.dto.strategy.UpsertSellStrategyRequest;
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
class SellStrategyServiceImplTest {

    @Mock
    private SellStrategyRepository sellStrategyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private SellStrategyServiceImpl sellStrategyService;

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
        lenient().when(sellStrategyRepository.save(any(SellStrategy.class))).thenAnswer(invocation -> {
            SellStrategy strategy = invocation.getArgument(0, SellStrategy.class);
            if (strategy.getId() == null) {
                strategy.setId(UUID.randomUUID());
            }
            return strategy;
        });
    }

    @Test
    void upsertCreatesStrategyWhenMissing() {
        when(sellStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId))
            .thenReturn(Optional.empty());

        SellStrategyResponse response = sellStrategyService.upsert(
            userId,
            new UpsertSellStrategyRequest(assetId, new BigDecimal("7.50"), null)
        );

        assertEquals(userId, response.userId());
        assertEquals(assetId, response.assetId());
        assertEquals(0, response.thresholdPercent().compareTo(new BigDecimal("7.50")));
        assertEquals(Boolean.TRUE, response.active());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    @Test
    void upsertUpdatesExistingStrategyForSameAsset() {
        SellStrategy existing = new SellStrategy();
        existing.setId(UUID.randomUUID());
        existing.setUser(user);
        existing.setAsset(asset);
        existing.setThresholdPercent(new BigDecimal("5.00"));
        existing.setActive(Boolean.TRUE);
        existing.setCreatedAt(java.time.OffsetDateTime.parse("2026-02-10T10:00:00Z"));
        existing.setUpdatedAt(java.time.OffsetDateTime.parse("2026-02-10T10:00:00Z"));

        when(sellStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId))
            .thenReturn(Optional.of(existing));

        SellStrategyResponse response = sellStrategyService.upsert(
            userId,
            new UpsertSellStrategyRequest(assetId, new BigDecimal("9.25"), Boolean.FALSE)
        );

        assertEquals(existing.getId(), response.id());
        assertEquals(0, response.thresholdPercent().compareTo(new BigDecimal("9.25")));
        assertEquals(Boolean.FALSE, response.active());
        assertEquals(existing.getCreatedAt(), response.createdAt());
    }

    @Test
    void upsertRejectsNonPositiveThreshold() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> sellStrategyService.upsert(
                userId,
                new UpsertSellStrategyRequest(assetId, BigDecimal.ZERO, Boolean.TRUE)
            )
        );

        assertEquals("thresholdPercent must be positive", ex.getMessage());
    }
}
