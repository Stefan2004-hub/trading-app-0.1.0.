package com.trading.dto.transaction;

import com.trading.domain.enums.BuyInputMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BuyTransactionRequest(
    @NotNull(message = "assetId is required")
    UUID assetId,
    @NotNull(message = "exchangeId is required")
    UUID exchangeId,
    BigDecimal grossAmount,
    @PositiveOrZero(message = "feeAmount must be zero or positive")
    BigDecimal feeAmount,
    @PositiveOrZero(message = "feePercentage must be zero or positive")
    BigDecimal feePercentage,
    @Size(max = 10, message = "feeCurrency must be at most 10 characters")
    String feeCurrency,
    @NotNull(message = "inputMode is required")
    BuyInputMode inputMode,
    @DecimalMin(value = "0.000000000000000001", message = "usdAmount must be positive")
    BigDecimal usdAmount,
    @NotNull(message = "unitPriceUsd is required")
    @DecimalMin(value = "0.000000000000000001", message = "unitPriceUsd must be positive")
    BigDecimal unitPriceUsd,
    OffsetDateTime transactionDate
) {
}
