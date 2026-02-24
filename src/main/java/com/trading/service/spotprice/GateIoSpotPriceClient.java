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
public class GateIoSpotPriceClient {

    private final SpotPriceProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GateIoSpotPriceClient(SpotPriceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.resolvedConnectTimeoutMillis()))
            .build();
    }

    public String fetchPriceUsd(String gateIoSymbol) {
        String currencyPair = gateIoSymbol + "_USDT";
        URI uri = UriComponentsBuilder
            .fromUriString(properties.resolvedGateIoBaseUrl())
            .path("/spot/tickers")
            .queryParam("currency_pair", currencyPair)
            .build(true)
            .toUri();
        String body = send(uri, "gateio");
        try {
            JsonNode payload = objectMapper.readTree(body);
            if (!payload.isArray() || payload.isEmpty()) {
                throw new SpotPriceProviderException(null, "Gate.io empty ticker payload");
            }
            String last = payload.get(0).path("last").asText(null);
            if (last == null || last.isBlank()) {
                throw new SpotPriceProviderException(null, "Gate.io last price missing");
            }
            return last;
        } catch (IOException ex) {
            throw new SpotPriceProviderException(null, "Gate.io response parse failure");
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
            throw new SpotPriceProviderException(null, "Gate.io network failure");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SpotPriceProviderException(null, "Gate.io request interrupted");
        }

        int statusCode = response.statusCode();
        if (statusCode >= 400) {
            throw new SpotPriceProviderException(statusCode, provider + " returned " + statusCode);
        }
        return response.body();
    }
}
