package com.trading.dto.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertExchangeRequest(
    @NotBlank(message = "symbol is required")
    @Size(max = 10, message = "symbol must be at most 10 characters")
    String symbol,
    @NotBlank(message = "name is required")
    @Size(max = 50, message = "name must be at most 50 characters")
    String name
) {
}
