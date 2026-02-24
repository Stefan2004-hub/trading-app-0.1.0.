package com.trading.dto.spotprice;

public record SpotPriceAttemptResponse(
    String provider,
    Integer status,
    String reason
) {
}
