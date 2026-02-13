package com.trading.domain.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface SellOpportunityProjection {

    UUID getUserId();

    UUID getTransactionId();

    String getSymbol();

    String getAssetName();

    String getTransactionType();

    BigDecimal getBuyPrice();

    BigDecimal getCoinAmount();

    BigDecimal getThresholdPercent();

    BigDecimal getTargetSellPrice();
}
