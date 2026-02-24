package com.trading.service.spotprice;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.spot-price")
public record SpotPriceProperties(
    String coinbaseBaseUrl,
    String gateIoBaseUrl,
    long cacheTtlMillis,
    long connectTimeoutMillis,
    long readTimeoutMillis
) {
    public String resolvedCoinbaseBaseUrl() {
        if (coinbaseBaseUrl == null || coinbaseBaseUrl.isBlank()) {
            return "https://api.coinbase.com";
        }
        return coinbaseBaseUrl;
    }

    public String resolvedGateIoBaseUrl() {
        if (gateIoBaseUrl == null || gateIoBaseUrl.isBlank()) {
            return "https://api.gateio.ws/api/v4";
        }
        return gateIoBaseUrl;
    }

    public long resolvedCacheTtlMillis() {
        return cacheTtlMillis > 0 ? cacheTtlMillis : 30_000L;
    }

    public long resolvedConnectTimeoutMillis() {
        return connectTimeoutMillis > 0 ? connectTimeoutMillis : 2_000L;
    }

    public long resolvedReadTimeoutMillis() {
        return readTimeoutMillis > 0 ? readTimeoutMillis : 3_000L;
    }
}
