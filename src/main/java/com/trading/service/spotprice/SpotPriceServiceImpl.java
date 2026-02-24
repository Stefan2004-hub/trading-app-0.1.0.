package com.trading.service.spotprice;

import com.trading.domain.entity.Asset;
import com.trading.domain.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SpotPriceServiceImpl implements SpotPriceService {

    private static final Logger LOG = LoggerFactory.getLogger(SpotPriceServiceImpl.class);

    private final CoinbaseSpotPriceClient coinbaseSpotPriceClient;
    private final GateIoSpotPriceClient gateIoSpotPriceClient;
    private final AssetRepository assetRepository;
    private final SpotPriceProperties properties;
    private final Map<String, CachedSpotPrice> cache = new ConcurrentHashMap<>();

    public SpotPriceServiceImpl(
        CoinbaseSpotPriceClient coinbaseSpotPriceClient,
        GateIoSpotPriceClient gateIoSpotPriceClient,
        ObjectProvider<AssetRepository> assetRepositoryProvider,
        SpotPriceProperties properties
    ) {
        this.coinbaseSpotPriceClient = coinbaseSpotPriceClient;
        this.gateIoSpotPriceClient = gateIoSpotPriceClient;
        this.assetRepository = assetRepositoryProvider.getIfAvailable();
        this.properties = properties;
    }

    @Override
    public SpotPriceResult resolveSpotPrice(String symbol) {
        String normalized = normalizeSymbol(symbol);
        CachedSpotPrice cached = cache.get(normalized);
        if (cached != null && cached.isFresh(properties.resolvedCacheTtlMillis())) {
            return cached.result();
        }

        List<ProviderAttempt> attempts = new ArrayList<>();
        try {
            String coinbasePrice = coinbaseSpotPriceClient.fetchPriceUsd(normalized);
            SpotPriceResult result = new SpotPriceResult(
                normalized,
                coinbasePrice,
                "coinbase",
                normalized + "-USD",
                OffsetDateTime.now()
            );
            cache.put(normalized, new CachedSpotPrice(result, System.currentTimeMillis()));
            return result;
        } catch (SpotPriceProviderException ex) {
            attempts.add(new ProviderAttempt("coinbase", ex.getStatusCode(), ex.getMessage()));
            LOG.warn("Coinbase spot lookup failed for {}: {}", normalized, ex.getMessage());
        }

        String gateSymbol = resolveGateIoSymbol(normalized);
        try {
            String gateIoPrice = gateIoSpotPriceClient.fetchPriceUsd(gateSymbol);
            SpotPriceResult result = new SpotPriceResult(
                normalized,
                gateIoPrice,
                "gateio",
                gateSymbol + "_USDT",
                OffsetDateTime.now()
            );
            cache.put(normalized, new CachedSpotPrice(result, System.currentTimeMillis()));
            return result;
        } catch (SpotPriceProviderException ex) {
            attempts.add(new ProviderAttempt("gateio", ex.getStatusCode(), ex.getMessage()));
            LOG.warn("Gate.io spot lookup failed for {} ({}): {}", normalized, gateSymbol, ex.getMessage());
        }

        throw new SpotPriceUnavailableException(normalized, List.copyOf(attempts));
    }

    private String resolveGateIoSymbol(String normalizedSymbol) {
        if (assetRepository == null) {
            return normalizedSymbol;
        }
        Asset asset = assetRepository.findBySymbolIgnoreCase(normalizedSymbol).orElse(null);
        if (asset == null || asset.getGateIoSymbol() == null || asset.getGateIoSymbol().isBlank()) {
            return normalizedSymbol;
        }
        return asset.getGateIoSymbol().trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol is required");
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        return normalized;
    }

    private record CachedSpotPrice(SpotPriceResult result, long fetchedAtMillis) {
        private boolean isFresh(long ttlMillis) {
            return System.currentTimeMillis() - fetchedAtMillis < ttlMillis;
        }
    }
}
