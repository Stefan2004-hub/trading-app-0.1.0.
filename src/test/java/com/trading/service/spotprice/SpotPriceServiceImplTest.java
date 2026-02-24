package com.trading.service.spotprice;

import com.trading.domain.entity.Asset;
import com.trading.domain.repository.AssetRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotPriceServiceImplTest {

    @Mock
    private CoinbaseSpotPriceClient coinbaseSpotPriceClient;
    @Mock
    private GateIoSpotPriceClient gateIoSpotPriceClient;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private ObjectProvider<AssetRepository> assetRepositoryProvider;

    private SpotPriceServiceImpl service;

    @BeforeEach
    void setUp() {
        SpotPriceProperties properties = new SpotPriceProperties(
            "https://api.coinbase.com",
            "https://api.gateio.ws/api/v4",
            30_000L,
            2_000L,
            3_000L
        );
        when(assetRepositoryProvider.getIfAvailable()).thenReturn(assetRepository);
        service = new SpotPriceServiceImpl(coinbaseSpotPriceClient, gateIoSpotPriceClient, assetRepositoryProvider, properties);
    }

    @Test
    void resolveSpotPriceUsesCoinbaseWhenAvailable() {
        when(coinbaseSpotPriceClient.fetchPriceUsd("BTC")).thenReturn("100000.00");

        SpotPriceResult result = service.resolveSpotPrice("btc");

        assertEquals("coinbase", result.source());
        assertEquals("100000.00", result.priceUsd());
        assertEquals("BTC-USD", result.resolvedPair());
        verify(gateIoSpotPriceClient, never()).fetchPriceUsd("BTC");
    }

    @Test
    void resolveSpotPriceFallsBackToGateIo() {
        when(coinbaseSpotPriceClient.fetchPriceUsd("ORDER"))
            .thenThrow(new SpotPriceProviderException(404, "coinbase returned 404"));
        when(assetRepository.findBySymbolIgnoreCase("ORDER")).thenReturn(Optional.empty());
        when(gateIoSpotPriceClient.fetchPriceUsd("ORDER")).thenReturn("0.1234");

        SpotPriceResult result = service.resolveSpotPrice("ORDER");

        assertEquals("gateio", result.source());
        assertEquals("ORDER_USDT", result.resolvedPair());
        assertEquals("0.1234", result.priceUsd());
    }

    @Test
    void resolveSpotPriceUsesGateIoAliasWhenConfigured() {
        when(coinbaseSpotPriceClient.fetchPriceUsd("ORDER"))
            .thenThrow(new SpotPriceProviderException(404, "coinbase returned 404"));
        Asset asset = new Asset();
        asset.setSymbol("ORDER");
        asset.setGateIoSymbol("ORDERLY");
        when(assetRepository.findBySymbolIgnoreCase("ORDER")).thenReturn(Optional.of(asset));
        when(gateIoSpotPriceClient.fetchPriceUsd("ORDERLY")).thenReturn("0.211");

        SpotPriceResult result = service.resolveSpotPrice("ORDER");

        assertEquals("gateio", result.source());
        assertEquals("ORDERLY_USDT", result.resolvedPair());
        verify(gateIoSpotPriceClient).fetchPriceUsd("ORDERLY");
    }

    @Test
    void resolveSpotPriceThrowsUnavailableWhenBothProvidersFail() {
        when(coinbaseSpotPriceClient.fetchPriceUsd("ORDER"))
            .thenThrow(new SpotPriceProviderException(404, "coinbase returned 404"));
        when(assetRepository.findBySymbolIgnoreCase("ORDER")).thenReturn(Optional.empty());
        when(gateIoSpotPriceClient.fetchPriceUsd("ORDER"))
            .thenThrow(new SpotPriceProviderException(404, "gateio returned 404"));

        SpotPriceUnavailableException ex = assertThrows(
            SpotPriceUnavailableException.class,
            () -> service.resolveSpotPrice("ORDER")
        );

        assertEquals("ORDER", ex.getSymbol());
        assertEquals(2, ex.getAttempts().size());
    }
}
