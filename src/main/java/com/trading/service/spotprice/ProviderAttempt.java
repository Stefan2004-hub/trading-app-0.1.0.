package com.trading.service.spotprice;

public record ProviderAttempt(
    String provider,
    Integer status,
    String reason
) {
}
