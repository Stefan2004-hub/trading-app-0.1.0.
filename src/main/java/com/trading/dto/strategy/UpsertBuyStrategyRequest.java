package com.trading.dto.strategy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpsertBuyStrategyRequest(
    @NotNull(message = "assetId is required")
    UUID assetId,
    @NotNull(message = "dipThresholdPercent is required")
    @DecimalMin(value = "0.01", message = "dipThresholdPercent must be positive")
    BigDecimal dipThresholdPercent,
    @NotNull(message = "buyAmountUsd is required")
    @DecimalMin(value = "0.01", message = "buyAmountUsd must be positive")
    BigDecimal buyAmountUsd,
    Boolean active
) {
}
