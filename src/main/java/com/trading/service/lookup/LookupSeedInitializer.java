package com.trading.service.lookup;

import com.trading.domain.entity.Asset;
import com.trading.domain.entity.Exchange;
import com.trading.domain.repository.AssetRepository;
import com.trading.domain.repository.ExchangeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.lookup.seed.enabled", havingValue = "true")
public class LookupSeedInitializer implements ApplicationRunner {

    private final AssetRepository assetRepository;
    private final ExchangeRepository exchangeRepository;

    public LookupSeedInitializer(AssetRepository assetRepository, ExchangeRepository exchangeRepository) {
        this.assetRepository = assetRepository;
        this.exchangeRepository = exchangeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureAssets();
        ensureExchanges();
    }

    private void ensureAssets() {
        List<SeedAsset> defaults = List.of(
            new SeedAsset("11111111-1111-1111-1111-111111111111", "BTC", "Bitcoin"),
            new SeedAsset("22222222-2222-2222-2222-222222222222", "ETH", "Ethereum"),
            new SeedAsset("33333333-3333-3333-3333-333333333333", "SOL", "Solana"),
            new SeedAsset("44444444-4444-4444-4444-444444444444", "ADA", "Cardano"),
            new SeedAsset("55555555-5555-5555-5555-555555555555", "BNB", "BNB")
        );

        for (SeedAsset defaultAsset : defaults) {
            if (assetRepository.findBySymbolIgnoreCase(defaultAsset.symbol()).isPresent()) {
                continue;
            }
            Asset asset = new Asset();
            asset.setId(UUID.fromString(defaultAsset.id()));
            asset.setSymbol(defaultAsset.symbol());
            asset.setName(defaultAsset.name());
            assetRepository.save(asset);
        }
    }

    private void ensureExchanges() {
        List<SeedExchange> defaults = List.of(
            new SeedExchange("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "BINANCE", "Binance"),
            new SeedExchange("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "COINBASE", "Coinbase"),
            new SeedExchange("cccccccc-cccc-cccc-cccc-cccccccccccc", "KRAKEN", "Kraken"),
            new SeedExchange("dddddddd-dddd-dddd-dddd-dddddddddddd", "BYBIT", "Bybit"),
            new SeedExchange("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee", "OKX", "OKX")
        );

        for (SeedExchange defaultExchange : defaults) {
            if (exchangeRepository.findByNameIgnoreCase(defaultExchange.name()).isPresent()
                || exchangeRepository.findBySymbolIgnoreCase(defaultExchange.symbol()).isPresent()) {
                continue;
            }
            Exchange exchange = new Exchange();
            exchange.setId(UUID.fromString(defaultExchange.id()));
            exchange.setSymbol(defaultExchange.symbol());
            exchange.setName(defaultExchange.name());
            exchangeRepository.save(exchange);
        }
    }

    private record SeedAsset(String id, String symbol, String name) {
    }

    private record SeedExchange(String id, String symbol, String name) {
    }
}
