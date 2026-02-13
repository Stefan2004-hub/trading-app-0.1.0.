package com.trading.domain.projection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface BuyOpportunityProjection {

    UUID getUserId();

    UUID getAssetId();

    String getSymbol();

    String getAssetName();

    BigDecimal getDipThresholdPercent();

    BigDecimal getBuyAmountUsd();

    BigDecimal getPeakPrice();

    BigDecimal getTargetBuyPrice();

    UUID getLastBuyTransactionId();

    OffsetDateTime getLastPeakTimestamp();
}
