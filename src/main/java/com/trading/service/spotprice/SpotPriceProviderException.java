package com.trading.service.spotprice;

public class SpotPriceProviderException extends RuntimeException {

    private final Integer statusCode;

    public SpotPriceProviderException(Integer statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
