package com.trading.dto.strategy;

import java.math.BigDecimal;
import java.util.UUID;

public record UpsertBuyStrategyRequest(
    UUID assetId,
    BigDecimal dipThresholdPercent,
    BigDecimal buyAmountUsd,
    Boolean active
) {
}
