package com.trading.dto.strategy;

import com.trading.domain.enums.StrategyAlertStatus;
import com.trading.domain.enums.StrategyType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StrategyAlertResponse(
    UUID id,
    UUID userId,
    UUID assetId,
    StrategyType strategyType,
    BigDecimal triggerPrice,
    BigDecimal thresholdPercent,
    BigDecimal referencePrice,
    String alertMessage,
    StrategyAlertStatus status,
    OffsetDateTime createdAt
) {
}
