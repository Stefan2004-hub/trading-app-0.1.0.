package com.trading.dto.strategy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BuyStrategyResponse(
    UUID id,
    UUID userId,
    UUID assetId,
    BigDecimal dipThresholdPercent,
    BigDecimal buyAmountUsd,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
