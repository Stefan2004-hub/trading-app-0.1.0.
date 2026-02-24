package com.trading.service.spotprice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class CoinbaseSpotPriceClient {

    private final SpotPriceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CoinbaseSpotPriceClient(SpotPriceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.resolvedConnectTimeoutMillis()))
            .build();
    }

    public String fetchPriceUsd(String symbol) {
        URI uri = UriComponentsBuilder
            .fromUriString(properties.resolvedCoinbaseBaseUrl())
            .path("/v2/prices/{symbol}-USD/spot")
            .buildAndExpand(symbol)
            .toUri();
        String body = send(uri, "coinbase");
        try {
            JsonNode payload = objectMapper.readTree(body);
            String amount = payload.path("data").path("amount").asText(null);
            if (amount == null || amount.isBlank()) {
                throw new SpotPriceProviderException(null, "Coinbase amount missing");
            }
            return amount;
        } catch (IOException ex) {
            throw new SpotPriceProviderException(null, "Coinbase response parse failure");
        }
    }

    private String send(URI uri, String provider) {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofMillis(properties.resolvedReadTimeoutMillis()))
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new SpotPriceProviderException(null, "Coinbase network failure");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SpotPriceProviderException(null, "Coinbase request interrupted");
        }

        int statusCode = response.statusCode();
        if (statusCode >= 400) {
            throw new SpotPriceProviderException(statusCode, provider + " returned " + statusCode);
        }
        return response.body();
    }
}
