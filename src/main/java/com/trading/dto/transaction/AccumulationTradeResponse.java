package com.trading.dto.transaction;

import com.trading.domain.enums.AccumulationTradeStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccumulationTradeResponse(
    UUID id,
    UUID userId,
    UUID assetId,
    UUID exitTransactionId,
    UUID reentryTransactionId,
    BigDecimal oldCoinAmount,
    BigDecimal newCoinAmount,
    BigDecimal accumulationDelta,
    AccumulationTradeStatus status,
    BigDecimal exitPriceUsd,
    BigDecimal reentryPriceUsd,
    OffsetDateTime createdAt,
    OffsetDateTime closedAt,
    String predictionNotes
) {
}
