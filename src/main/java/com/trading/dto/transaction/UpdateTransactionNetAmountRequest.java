package com.trading.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateTransactionNetAmountRequest(
    @NotNull(message = "netAmount is required")
    @DecimalMin(value = "0.000000000000000001", message = "netAmount must be positive")
    BigDecimal netAmount
) {
}
