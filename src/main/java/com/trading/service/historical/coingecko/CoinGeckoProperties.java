package com.trading.service.historical.coingecko;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.coingecko")
public record CoinGeckoProperties(
    String baseUrl,
    String apiKey
) {
    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.coingecko.com/api/v3";
        }
        return baseUrl;
    }
}
