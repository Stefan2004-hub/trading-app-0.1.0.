package com.trading.dto.strategy;

import java.math.BigDecimal;
import java.util.UUID;

public record GenerateStrategyAlertsRequest(
    UUID assetId,
    BigDecimal currentPriceUsd
) {
}
