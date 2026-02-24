package com.trading.service.spotprice;

import java.time.OffsetDateTime;

public record SpotPriceResult(
    String symbol,
    String priceUsd,
    String source,
    String resolvedPair,
    OffsetDateTime fetchedAt
) {
}
