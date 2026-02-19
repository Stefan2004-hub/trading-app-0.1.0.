package com.trading.dto.transaction;

import java.math.BigDecimal;

public record UpdateTransactionRequest(
    BigDecimal grossAmount,
    BigDecimal feeAmount,
    BigDecimal feePercentage,
    BigDecimal unitPriceUsd
) {
}
