package com.trading.dto.portfolio;

import java.math.BigDecimal;

public record PortfolioAssetPerformanceResponse(
    String symbol,
    String exchange,
    BigDecimal currentBalance,
    BigDecimal totalInvestedUsd,
    BigDecimal avgBuyPrice,
    BigDecimal currentUnitPriceUsd,
    BigDecimal currentValueUsd,
    BigDecimal unrealizedPnlUsd,
    BigDecimal unrealizedPnlPercent,
    BigDecimal realizedPnlUsd,
    BigDecimal totalPnlUsd
) {
}
