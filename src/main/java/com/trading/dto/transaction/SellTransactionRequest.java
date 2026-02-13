package com.trading.dto.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SellTransactionRequest(
    UUID assetId,
    UUID exchangeId,
    BigDecimal grossAmount,
    BigDecimal feeAmount,
    String feeCurrency,
    BigDecimal unitPriceUsd,
    OffsetDateTime transactionDate
) {
}
