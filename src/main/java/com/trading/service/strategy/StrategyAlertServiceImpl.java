package com.trading.service.strategy;

import com.trading.domain.entity.BuyStrategy;
import com.trading.domain.entity.PricePeak;
import com.trading.domain.entity.SellStrategy;
import com.trading.domain.entity.StrategyAlert;
import com.trading.domain.entity.Transaction;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class StrategyAlertServiceImpl implements StrategyAlertService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final StrategyAlertRepository strategyAlertRepository;
    private final SellStrategyRepository sellStrategyRepository;
    private final BuyStrategyRepository buyStrategyRepository;
    private final TransactionRepository transactionRepository;
    private final PricePeakRepository pricePeakRepository;

    public StrategyAlertServiceImpl(
        StrategyAlertRepository strategyAlertRepository,
        SellStrategyRepository sellStrategyRepository,
        BuyStrategyRepository buyStrategyRepository,
        TransactionRepository transactionRepository,
        PricePeakRepository pricePeakRepository
    ) {
        this.strategyAlertRepository = strategyAlertRepository;
        this.sellStrategyRepository = sellStrategyRepository;
        this.buyStrategyRepository = buyStrategyRepository;
        this.transactionRepository = transactionRepository;
        this.pricePeakRepository = pricePeakRepository;
    }

    @Override
    public List<StrategyAlertResponse> list(UUID userId) {
        Objects.requireNonNull(userId, "userId is required");
        return strategyAlertRepository.findAllByUser_IdOrderByCreatedAtDesc(userId).stream()
            .map(StrategyAlertServiceImpl::toResponse)
            .toList();
    }

    @Override
    public List<StrategyAlertResponse> generate(UUID userId, GenerateStrategyAlertsRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.assetId(), "assetId is required");
        Objects.requireNonNull(request.currentPriceUsd(), "currentPriceUsd is required");
        if (request.currentPriceUsd().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("currentPriceUsd must be positive");
        }

        List<StrategyAlertResponse> generated = new ArrayList<>();
        maybeGenerateSellAlert(userId, request.assetId(), request.currentPriceUsd(), generated);
        maybeGenerateBuyAlert(userId, request.assetId(), request.currentPriceUsd(), generated);
        return generated;
    }

    @Override
    public StrategyAlertResponse acknowledge(UUID userId, UUID alertId) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(alertId, "alertId is required");

        StrategyAlert alert = strategyAlertRepository.findByIdAndUser_Id(alertId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Strategy alert not found: " + alertId));

        if (alert.getStatus() != StrategyAlertStatus.ACKNOWLEDGED) {
            alert.setStatus(StrategyAlertStatus.ACKNOWLEDGED);
            alert.setAcknowledgedAt(OffsetDateTime.now());
            alert = strategyAlertRepository.save(alert);
        }
        return toResponse(alert);
    }

    private void maybeGenerateSellAlert(
        UUID userId,
        UUID assetId,
        BigDecimal currentPrice,
        List<StrategyAlertResponse> generated
    ) {
        SellStrategy strategy = sellStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId).orElse(null);
        if (strategy == null || !Boolean.TRUE.equals(strategy.getActive())) {
            return;
        }
        List<Transaction> buys =
            transactionRepository.findAllByUser_IdAndAsset_IdAndTransactionTypeOrderByTransactionDateDesc(
                userId,
                assetId,
                TransactionType.BUY
            );
        if (buys.isEmpty()) {
            return;
        }

        BigDecimal referencePrice = buys.get(0).getUnitPriceUsd();
        BigDecimal targetPrice = referencePrice.multiply(
            BigDecimal.ONE.add(strategy.getThresholdPercent().divide(ONE_HUNDRED))
        );
        if (currentPrice.compareTo(targetPrice) < 0) {
            return;
        }

        boolean pendingExists = strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId,
            assetId,
            StrategyType.SELL,
            StrategyAlertStatus.PENDING
        );
        if (pendingExists) {
            return;
        }

        StrategyAlert alert = new StrategyAlert();
        alert.setUser(strategy.getUser());
        alert.setAsset(strategy.getAsset());
        alert.setStrategyType(StrategyType.SELL);
        alert.setTriggerPrice(currentPrice);
        alert.setThresholdPercent(strategy.getThresholdPercent());
        alert.setReferencePrice(referencePrice);
        alert.setAlertMessage("Sell threshold reached for " + strategy.getAsset().getSymbol());
        alert.setStatus(StrategyAlertStatus.PENDING);
        alert.setCreatedAt(OffsetDateTime.now());
        StrategyAlert saved = strategyAlertRepository.save(alert);
        generated.add(toResponse(saved));
    }

    private void maybeGenerateBuyAlert(
        UUID userId,
        UUID assetId,
        BigDecimal currentPrice,
        List<StrategyAlertResponse> generated
    ) {
        BuyStrategy strategy = buyStrategyRepository.findByUser_IdAndAsset_Id(userId, assetId).orElse(null);
        if (strategy == null || !Boolean.TRUE.equals(strategy.getActive())) {
            return;
        }
        PricePeak pricePeak = pricePeakRepository.findByUser_IdAndAsset_IdAndActiveTrue(userId, assetId).orElse(null);
        if (pricePeak == null) {
            return;
        }

        BigDecimal referencePrice = pricePeak.getPeakPrice();
        BigDecimal targetPrice = referencePrice.multiply(
            BigDecimal.ONE.subtract(strategy.getDipThresholdPercent().divide(ONE_HUNDRED))
        );
        if (currentPrice.compareTo(targetPrice) > 0) {
            return;
        }

        boolean pendingExists = strategyAlertRepository.existsByUser_IdAndAsset_IdAndStrategyTypeAndStatus(
            userId,
            assetId,
            StrategyType.BUY,
            StrategyAlertStatus.PENDING
        );
        if (pendingExists) {
            return;
        }

        StrategyAlert alert = new StrategyAlert();
        alert.setUser(strategy.getUser());
        alert.setAsset(strategy.getAsset());
        alert.setStrategyType(StrategyType.BUY);
        alert.setTriggerPrice(currentPrice);
        alert.setThresholdPercent(strategy.getDipThresholdPercent());
        alert.setReferencePrice(referencePrice);
        alert.setAlertMessage("Buy dip threshold reached for " + strategy.getAsset().getSymbol());
        alert.setStatus(StrategyAlertStatus.PENDING);
        alert.setCreatedAt(OffsetDateTime.now());
        StrategyAlert saved = strategyAlertRepository.save(alert);
        generated.add(toResponse(saved));
    }

    private static StrategyAlertResponse toResponse(StrategyAlert alert) {
        return new StrategyAlertResponse(
            alert.getId(),
            alert.getUser().getId(),
            alert.getAsset().getId(),
            alert.getStrategyType(),
            alert.getTriggerPrice(),
            alert.getThresholdPercent(),
            alert.getReferencePrice(),
            alert.getAlertMessage(),
            alert.getStatus(),
            alert.getCreatedAt(),
            alert.getAcknowledgedAt(),
            alert.getExecutedAt()
        );
    }
}
