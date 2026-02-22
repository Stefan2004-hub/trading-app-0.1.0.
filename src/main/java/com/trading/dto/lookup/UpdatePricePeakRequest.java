package com.trading.dto.lookup;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record UpdatePricePeakRequest(
    @NotNull(message = "peakPrice is required")
    @DecimalMin(value = "0.00000001", message = "peakPrice must be positive")
    BigDecimal peakPrice,
    @NotNull(message = "peakTimestamp is required")
    OffsetDateTime peakTimestamp,
    @NotNull(message = "active is required")
    Boolean active
) {
}
