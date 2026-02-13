package com.trading.dto.transaction;

import com.trading.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID userId,
    UUID assetId,
    UUID exchangeId,
    TransactionType transactionType,
    BigDecimal grossAmount,
    BigDecimal feeAmount,
    String feeCurrency,
    BigDecimal netAmount,
    BigDecimal unitPriceUsd,
    BigDecimal totalSpentUsd,
    BigDecimal realizedPnl,
    OffsetDateTime transactionDate
) {
}
