package com.trading.dto.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryResponse(
    BigDecimal totalInvestedUsd,
    BigDecimal totalCurrentValueUsd,
    BigDecimal totalUnrealizedPnlUsd,
    BigDecimal totalUnrealizedPnlPercent,
    BigDecimal totalRealizedPnlUsd,
    BigDecimal totalPnlUsd,
    List<PortfolioAssetPerformanceResponse> assets
) {
}
