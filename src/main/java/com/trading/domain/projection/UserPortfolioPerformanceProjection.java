package com.trading.domain.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface UserPortfolioPerformanceProjection {

    UUID getUserId();

    String getSymbol();

    String getExchange();

    BigDecimal getCurrentBalance();

    BigDecimal getTotalInvestedUsd();

    BigDecimal getAvgBuyPrice();
}
