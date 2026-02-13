package com.trading.service.strategy;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.BuyStrategy;
import com.trading.domain.entity.User;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.BuyStrategyRepository;
import com.trading.domain.repository.UserRepository;
import com.trading.dto.strategy.BuyStrategyResponse;
import com.trading.dto.strategy.UpsertBuyStrategyRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnBean({BuyStrategyRepository.class, UserRepository.class, AssetRepository.class})
public class BuyStrategyServiceImpl implements BuyStrategyService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final BuyStrategyRepository buyStrategyRepository;
    private final UserRepository userRepository;
    private final AssetRepository assetRepository;

    public BuyStrategyServiceImpl(
        BuyStrategyRepository buyStrategyRepository,
        UserRepository userRepository,
        AssetRepository assetRepository
    ) {
        this.buyStrategyRepository = buyStrategyRepository;
        this.userRepository = userRepository;
        this.assetRepository = assetRepository;
    }

    @Override
    public List<BuyStrategyResponse> list(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        return buyStrategyRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId).stream()
            .map(BuyStrategyServiceImpl::toResponse)
            .toList();
    }

    @Override
    public BuyStrategyResponse upsert(UUID userId, UpsertBuyStrategyRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.assetId(), "assetId is required");
        Objects.requireNonNull(request.dipThresholdPercent(), "dipThresholdPercent is required");
        Objects.requireNonNull(request.buyAmountUsd(), "buyAmountUsd is required");

        if (request.dipThresholdPercent().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("dipThresholdPercent must be positive");
        }
        if (request.buyAmountUsd().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("buyAmountUsd must be positive");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Asset asset = assetRepository.findById(request.assetId())
            .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + request.assetId()));

        OffsetDateTime now = OffsetDateTime.now();
        BuyStrategy strategy = buyStrategyRepository.findByUser_IdAndAsset_Id(userId, request.assetId())
            .orElseGet(() -> {
                BuyStrategy created = new BuyStrategy();
                created.setUser(user);
                created.setAsset(asset);
                created.setCreatedAt(now);
                return created;
            });

        strategy.setDipThresholdPercent(request.dipThresholdPercent());
        strategy.setBuyAmountUsd(request.buyAmountUsd());
        if (request.active() != null) {
            strategy.setActive(request.active());
        } else if (strategy.getActive() == null) {
            strategy.setActive(Boolean.TRUE);
        }
        strategy.setUpdatedAt(now);

        BuyStrategy saved = buyStrategyRepository.save(strategy);
        return toResponse(saved);
    }

    private static BuyStrategyResponse toResponse(BuyStrategy strategy) {
        return new BuyStrategyResponse(
            strategy.getId(),
            strategy.getUser().getId(),
            strategy.getAsset().getId(),
            strategy.getDipThresholdPercent(),
            strategy.getBuyAmountUsd(),
            strategy.getActive(),
            strategy.getCreatedAt(),
            strategy.getUpdatedAt()
        );
    }
}
