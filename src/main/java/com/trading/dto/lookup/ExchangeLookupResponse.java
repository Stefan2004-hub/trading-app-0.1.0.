package com.trading.dto.lookup;

import java.util.UUID;

public record ExchangeLookupResponse(
    UUID id,
    String symbol,
    String name
) {
}
