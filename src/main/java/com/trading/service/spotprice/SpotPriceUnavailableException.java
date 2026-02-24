package com.trading.service.spotprice;

import java.util.List;

public class SpotPriceUnavailableException extends RuntimeException {

    private final String symbol;
    private final List<ProviderAttempt> attempts;

    public SpotPriceUnavailableException(String symbol, List<ProviderAttempt> attempts) {
        super("Spot price unavailable for symbol: " + symbol);
        this.symbol = symbol;
        this.attempts = attempts;
    }

    public String getSymbol() {
        return symbol;
    }

    public List<ProviderAttempt> getAttempts() {
        return attempts;
    }
}
