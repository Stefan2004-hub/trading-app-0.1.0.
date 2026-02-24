package com.trading.dto.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertAssetRequest(
    @NotBlank(message = "symbol is required")
    @Size(max = 10, message = "symbol must be at most 10 characters")
    String symbol,
    @NotBlank(message = "name is required")
    @Size(max = 50, message = "name must be at most 50 characters")
    String name,
    @Size(max = 120, message = "coinGeckoId must be at most 120 characters")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "coinGeckoId may contain only letters, numbers, and dashes")
    String coinGeckoId,
    @Size(max = 20, message = "gateIoSymbol must be at most 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "gateIoSymbol may contain only letters, numbers, and underscore")
    String gateIoSymbol
) {
}
