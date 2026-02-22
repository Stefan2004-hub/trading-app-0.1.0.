package com.trading.dto.marketalert;

import com.trading.domain.enums.MarketAlertStrategyType;
import com.trading.domain.enums.MarketAlertType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MarketAlertResponse(
    UUID id,
    UUID assetId,
    String assetSymbol,
    String assetName,
    MarketAlertType alertType,
    MarketAlertStrategyType strategyType,
    BigDecimal rsiValue,
    BigDecimal stochValue,
    Integer intervalDays,
    LocalDate triggerDate,
    Boolean isViewed,
    OffsetDateTime createdAt
) {
}
