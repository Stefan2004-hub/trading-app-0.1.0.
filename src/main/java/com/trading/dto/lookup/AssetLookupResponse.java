package com.trading.dto.lookup;

import java.util.UUID;

public record AssetLookupResponse(
    UUID id,
    String symbol,
    String name
) {
}
