package com.trading.domain.projection;

import java.math.BigDecimal;

public interface UserAssetRealizedPnlProjection {

    String getSymbol();

    String getExchange();

    BigDecimal getRealizedPnl();
}
