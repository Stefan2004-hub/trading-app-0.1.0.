package com.trading.dto.portfolio;

import java.math.BigDecimal;

public record AssetSummaryDTO(
    String assetName,
    String assetSymbol,
    BigDecimal netQuantity,
    BigDecimal totalInvested,
    BigDecimal totalRealizedProfit
) {
}
