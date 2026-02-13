package com.trading.dto.strategy;

import java.math.BigDecimal;
import java.util.UUID;

public record UpsertSellStrategyRequest(
    UUID assetId,
    BigDecimal thresholdPercent,
    Boolean active
) {
}
