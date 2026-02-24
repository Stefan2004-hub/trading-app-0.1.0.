package com.trading.dto.spotprice;

import java.time.OffsetDateTime;

public record SpotPriceResponse(
    String symbol,
    String priceUsd,
    String source,
    String resolvedPair,
    OffsetDateTime fetchedAt
) {
}
