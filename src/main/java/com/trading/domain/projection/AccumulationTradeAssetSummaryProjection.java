package com.trading.domain.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccumulationTradeAssetSummaryProjection {

    UUID getAssetId();

    BigDecimal getTotalAccumulationDelta();

    long getTradeCount();
}
