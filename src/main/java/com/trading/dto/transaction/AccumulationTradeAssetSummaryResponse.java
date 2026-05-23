package com.trading.dto.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record AccumulationTradeAssetSummaryResponse(
    UUID assetId,
    BigDecimal totalAccumulationDelta,
    long tradeCount
) {
}
