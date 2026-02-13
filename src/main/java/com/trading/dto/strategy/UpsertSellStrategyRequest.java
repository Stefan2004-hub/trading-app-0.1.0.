package com.trading.dto.strategy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpsertSellStrategyRequest(
    @NotNull(message = "assetId is required")
    UUID assetId,
    @NotNull(message = "thresholdPercent is required")
    @DecimalMin(value = "0.01", message = "thresholdPercent must be positive")
    BigDecimal thresholdPercent,
    Boolean active
) {
}
