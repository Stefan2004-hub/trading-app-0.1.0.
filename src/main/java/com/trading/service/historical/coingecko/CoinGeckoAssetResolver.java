package com.trading.service.historical.coingecko;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CoinGeckoAssetResolver {

    private static final Map<String, String> CANONICAL_IDS_BY_SYMBOL = Map.of(
        "BTC", "bitcoin",
        "ETH", "ethereum",
        "SOL", "solana",
        "ADA", "cardano",
        "BNB", "binancecoin"
    );

    private final CoinGeckoClient coinGeckoClient;

    private volatile Map<String, List<CoinGeckoClient.CoinGeckoCoin>> coinsBySymbol;

    public CoinGeckoAssetResolver(CoinGeckoClient coinGeckoClient) {
        this.coinGeckoClient = coinGeckoClient;
    }

    public String resolveCoinId(String assetSymbol, String assetName) {
        String normalizedSymbol = normalizeSymbol(assetSymbol);
        if (normalizedSymbol == null) {
            throw new IllegalArgumentException("asset symbol is empty");
        }

        List<CoinGeckoClient.CoinGeckoCoin> matches = getCoinsBySymbol()
            .getOrDefault(normalizedSymbol, List.of());
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("no CoinGecko mapping for symbol " + normalizedSymbol);
        }
        if (matches.size() == 1) {
            return matches.get(0).id();
        }

        String canonicalId = CANONICAL_IDS_BY_SYMBOL.get(normalizedSymbol);
        if (canonicalId != null) {
            for (CoinGeckoClient.CoinGeckoCoin match : matches) {
                if (canonicalId.equalsIgnoreCase(match.id())) {
                    return match.id();
                }
            }
        }

        String normalizedName = normalizeName(assetName);
        if (normalizedName != null) {
            List<CoinGeckoClient.CoinGeckoCoin> nameMatches = matches.stream()
                .filter(coin -> normalizedName.equals(normalizeName(coin.name())))
                .toList();
            if (nameMatches.size() == 1) {
                return nameMatches.get(0).id();
            }
        }

        String candidates = matches.stream()
            .limit(3)
            .map(CoinGeckoClient.CoinGeckoCoin::id)
            .reduce((left, right) -> left + ", " + right)
            .orElse("none");
        throw new IllegalArgumentException("ambiguous CoinGecko mapping for " + normalizedSymbol + " (" + candidates + ")");
    }

    private Map<String, List<CoinGeckoClient.CoinGeckoCoin>> getCoinsBySymbol() {
        Map<String, List<CoinGeckoClient.CoinGeckoCoin>> cached = coinsBySymbol;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (coinsBySymbol != null) {
                return coinsBySymbol;
            }
            Map<String, List<CoinGeckoClient.CoinGeckoCoin>> map = new HashMap<>();
            for (CoinGeckoClient.CoinGeckoCoin coin : coinGeckoClient.fetchCoinsList()) {
                String key = normalizeSymbol(coin.symbol());
                if (key == null) {
                    continue;
                }
                map.computeIfAbsent(key, ignored -> new ArrayList<>()).add(coin);
            }
            coinsBySymbol = map;
            return map;
        }
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return normalized.isBlank() ? null : normalized;
    }
}
