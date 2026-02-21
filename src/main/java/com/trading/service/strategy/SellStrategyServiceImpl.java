package com.trading.service.strategy;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.SellStrategy;
import com.trading.domain.entity.User;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.SellStrategyRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.strategy.SellStrategyResponse;
import com.trading.dto.strategy.UpsertSellStrategyRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SellStrategyServiceImpl implements SellStrategyService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final SellStrategyRepository sellStrategyRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;

    public SellStrategyServiceImpl(
        SellStrategyRepository sellStrategyRepository,
        UserRepository userRepository,
        AssetRepository assetRepository
    ) {
        this.sellStrategyRepository = sellStrategyRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
    }

    @Override
    public List<SellStrategyResponse> list(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        return sellStrategyRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId).stream()
            .map(SellStrategyServiceImpl::toResponse)
            .toList();
    }

    @Override
    public SellStrategyResponse upsert(UUID userId, UpsertSellStrategyRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.assetId(), "assetId is required");
        Objects.requireNonNull(request.thresholdPercent(), "thresholdPercent is required");
        if (request.thresholdPercent().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("thresholdPercent must be positive");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Asset asset = assetRepository.findById(request.assetId())
            .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));

        OffsetDateTime now = OffsetDateTime.now();
        SellStrategy strategy = sellStrategyRepository.findByUser_IdAndAsset_Id(userId, request.assetId())
            .orElseGet(() -> {
                SellStrategy created = new SellStrategy();
                created.setUser(user);
                created.setAsset(asset);
                created.setCreatedAt(now);
                return created;
            });

        strategy.setThresholdPercent(request.thresholdPercent());
        if (request.active() != null) {
            strategy.setActive(request.active());
        } else if (strategy.getActive() == null) {
            strategy.setActive(Boolean.TRUE);
        }
        strategy.setUpdatedAt(now);

        SellStrategy saved = sellStrategyRepository.save(strategy);
        return toResponse(saved);
    }

    @Override
    public void delete(UUID userId, UUID strategyId) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(strategyId, "strategyId is required");

        SellStrategy strategy = sellStrategyRepository.findByIdAndUser_Id(strategyId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Sell strategy not found: " + strategyId));
        sellStrategyRepository.delete(strategy);
    }

    private static SellStrategyResponse toResponse(SellStrategy strategy) {
        return new SellStrategyResponse(
            strategy.getId(),
            strategy.getUser().getId(),
            strategy.getAsset().getId(),
            strategy.getThresholdPercent(),
            strategy.getActive(),
            strategy.getCreatedAt(),
            strategy.getUpdatedAt()
        );
    }
}
