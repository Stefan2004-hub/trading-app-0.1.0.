package com.trading.service.lookup;

import com.trading.domain.entity.Asset;
import com.trading.domain.repository.AssetRepository;
import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.UpsertAssetRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;

    public AssetServiceImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public List<AssetLookupResponse> list(String search) {
        String normalized = normalizeSearch(search);
        List<Asset> rows = normalized == null
            ? assetRepository.findAllByOrderBySymbolAsc()
            : assetRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCaseOrderBySymbolAsc(
                normalized,
                normalized
            );
        return rows.stream()
            .map(AssetServiceImpl::toResponse)
            .toList();
    }

    @Override
    public AssetLookupResponse get(UUID id) {
        Objects.requireNonNull(id, "id is required");
        return toResponse(requireAsset(id));
    }

    @Override
    public AssetLookupResponse create(UpsertAssetRequest request) {
        Objects.requireNonNull(request, "request is required");
        String symbol = normalizeSymbol(request.symbol());
        String name = normalizeName(request.name());
        String coinGeckoId = normalizeCoinGeckoId(request.coinGeckoId());
        String gateIoSymbol = normalizeGateIoSymbol(request.gateIoSymbol());

        assertUniqueSymbol(symbol, null);
        assertUniqueCoinGeckoId(coinGeckoId, null);

        Asset asset = new Asset();
        asset.setSymbol(symbol);
        asset.setName(name);
        asset.setCoinGeckoId(coinGeckoId);
        asset.setGateIoSymbol(gateIoSymbol);
        return toResponse(assetRepository.save(asset));
    }

    @Override
    public AssetLookupResponse update(UUID id, UpsertAssetRequest request) {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(request, "request is required");
        String symbol = normalizeSymbol(request.symbol());
        String name = normalizeName(request.name());
        String coinGeckoId = normalizeCoinGeckoId(request.coinGeckoId());
        String gateIoSymbol = normalizeGateIoSymbol(request.gateIoSymbol());

        Asset existing = requireAsset(id);
        assertUniqueSymbol(symbol, id);
        assertUniqueCoinGeckoId(coinGeckoId, id);

        existing.setSymbol(symbol);
        existing.setName(name);
        existing.setCoinGeckoId(coinGeckoId);
        existing.setGateIoSymbol(gateIoSymbol);
        return toResponse(assetRepository.save(existing));
    }

    @Override
    public void delete(UUID id) {
        Objects.requireNonNull(id, "id is required");
        Asset asset = requireAsset(id);
        assetRepository.delete(asset);
    }

    private Asset requireAsset(UUID id) {
        return assetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + id));
    }

    private void assertUniqueSymbol(String symbol, UUID currentId) {
        assetRepository.findBySymbolIgnoreCase(symbol)
            .filter((row) -> !row.getId().equals(currentId))
            .ifPresent((row) -> {
                throw new IllegalArgumentException("Asset symbol already exists: " + symbol);
            });
    }

    private void assertUniqueCoinGeckoId(String coinGeckoId, UUID currentId) {
        if (coinGeckoId == null) {
            return;
        }
        assetRepository.findByCoinGeckoIdIgnoreCase(coinGeckoId)
            .filter((row) -> !row.getId().equals(currentId))
            .ifPresent((row) -> {
                throw new IllegalArgumentException("Asset CoinGecko ID already exists: " + coinGeckoId);
            });
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeName(String name) {
        return name == null ? null : name.trim();
    }

    private static String normalizeCoinGeckoId(String coinGeckoId) {
        if (coinGeckoId == null) {
            return null;
        }
        String normalized = coinGeckoId.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizeGateIoSymbol(String gateIoSymbol) {
        if (gateIoSymbol == null) {
            return null;
        }
        String normalized = gateIoSymbol.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private static AssetLookupResponse toResponse(Asset asset) {
        return new AssetLookupResponse(
            asset.getId(),
            asset.getSymbol(),
            asset.getName(),
            asset.getCoinGeckoId(),
            asset.getGateIoSymbol()
        );
    }
}
