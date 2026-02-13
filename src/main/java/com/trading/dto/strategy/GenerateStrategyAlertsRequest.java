package com.trading.dto.strategy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record GenerateStrategyAlertsRequest(
    @NotNull(message = "assetId is required")
    UUID assetId,
    @NotNull(message = "currentPriceUsd is required")
    @DecimalMin(value = "0.00000001", message = "currentPriceUsd must be positive")
    BigDecimal currentPriceUsd
) {
}
