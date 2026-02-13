package com.trading.dto.strategy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SellStrategyResponse(
    UUID id,
    UUID userId,
    UUID assetId,
    BigDecimal thresholdPercent,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
