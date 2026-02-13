package com.trading.domain.projection;

import java.math.BigDecimal;

public interface UserAssetLatestPriceProjection {

    String getSymbol();

    String getExchange();

    BigDecimal getLatestUnitPriceUsd();
}
