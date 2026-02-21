package com.trading.domain.projection;

import java.math.BigDecimal;

public interface UserAssetSummaryProjection {

    String getAssetName();

    String getAssetSymbol();

    BigDecimal getNetQuantity();

    BigDecimal getTotalInvested();

    BigDecimal getTotalRealizedProfit();
}
