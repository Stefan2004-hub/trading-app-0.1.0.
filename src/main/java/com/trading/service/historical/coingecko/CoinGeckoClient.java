package com.trading.service.historical.coingecko;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CoinGeckoClient {

    private final CoinGeckoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CoinGeckoClient(
        CoinGeckoProperties properties,
        ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<CoinGeckoCoin> fetchCoinsList() {
        URI uri = UriComponentsBuilder
            .fromUriString(properties.resolvedBaseUrl())
            .path("/coins/list")
            .queryParam("include_platform", "false")
            .build(true)
            .toUri();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .GET();
        applyApiKeyIfPresent(requestBuilder);

        String body = send(requestBuilder.build(), "CoinGecko coins list");
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) {
                return List.of();
            }
            List<CoinGeckoCoin> rows = new ArrayList<>();
            for (JsonNode node : root) {
                String id = textOrNull(node.path("id"));
                String symbol = textOrNull(node.path("symbol"));
                String name = textOrNull(node.path("name"));
                if (id == null || symbol == null || name == null) {
                    continue;
                }
                rows.add(new CoinGeckoCoin(id, symbol, name));
            }
            return rows;
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from CoinGecko API.", ex);
        }
    }

    public List<CoinGeckoDailyQuote> fetchDailyQuotes(
        String coinId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        long fromEpochSecond = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long toEpochSecond = endDate.plusDays(1L).atStartOfDay().toEpochSecond(ZoneOffset.UTC) - 1L;

        URI uri = UriComponentsBuilder
            .fromUriString(properties.resolvedBaseUrl())
            .path("/coins/{id}/market_chart/range")
            .queryParam("vs_currency", "usd")
            .queryParam("from", fromEpochSecond)
            .queryParam("to", toEpochSecond)
            .buildAndExpand(coinId)
            .toUri();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
            .header("Accept", "application/json")
            .GET();
        applyApiKeyIfPresent(requestBuilder);

        String body = send(requestBuilder.build(), "CoinGecko market chart");

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode prices = root.path("prices");
            if (!prices.isArray()) {
                return List.of();
            }

            Map<LocalDate, DayAggregator> byDay = new LinkedHashMap<>();
            for (JsonNode pointNode : prices) {
                if (!pointNode.isArray() || pointNode.size() < 2) {
                    continue;
                }

                JsonNode timeNode = pointNode.get(0);
                JsonNode priceNode = pointNode.get(1);
                if (timeNode == null || priceNode == null || !timeNode.isNumber()) {
                    continue;
                }

                BigDecimal price = decimalOrNull(priceNode);
                if (price == null) {
                    continue;
                }

                long timestampMillis = timeNode.asLong();
                LocalDate dayDate = Instant.ofEpochMilli(timestampMillis).atOffset(ZoneOffset.UTC).toLocalDate();
                if (dayDate.isBefore(startDate) || dayDate.isAfter(endDate)) {
                    continue;
                }

                byDay.computeIfAbsent(dayDate, ignored -> new DayAggregator()).accept(price);
            }

            return byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> entry.getValue().toQuote(entry.getKey()))
                .toList();
        } catch (IOException | RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from CoinGecko API.", ex);
        }
    }

    private String send(HttpRequest request, String operationName) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to call " + operationName + ".", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, operationName + " request was interrupted.", ex);
        }

        int statusCode = response.statusCode();
        if (statusCode == HttpStatus.TOO_MANY_REQUESTS.value()) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "CoinGecko rate limit reached. Please retry later."
            );
        }
        if (statusCode >= 400) {
            HttpStatusCode httpStatusCode = HttpStatusCode.valueOf(statusCode);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "CoinGecko API error: " + httpStatusCode);
        }
        return response.body();
    }

    private void applyApiKeyIfPresent(HttpRequest.Builder requestBuilder) {
        String apiKey = properties.apiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            requestBuilder.header("x-cg-demo-api-key", apiKey);
        }
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText(null);
        if (text == null || text.isBlank()) {
            return null;
        }
        return text;
    }

    private static BigDecimal decimalOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asText(null);
        if (text == null || text.isBlank()) {
            return null;
        }
        return new BigDecimal(text);
    }

    public record CoinGeckoCoin(
        String id,
        String symbol,
        String name
    ) {
    }

    public record CoinGeckoDailyQuote(
        LocalDate dayDate,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice
    ) {
    }

    private static final class DayAggregator {
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;

        private void accept(BigDecimal price) {
            if (open == null) {
                open = price;
                high = price;
                low = price;
                close = price;
                return;
            }
            if (price.compareTo(high) > 0) {
                high = price;
            }
            if (price.compareTo(low) < 0) {
                low = price;
            }
            close = price;
        }

        private CoinGeckoDailyQuote toQuote(LocalDate dayDate) {
            return new CoinGeckoDailyQuote(dayDate, high, low, close);
        }
    }
}
