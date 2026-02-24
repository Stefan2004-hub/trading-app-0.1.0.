package com.trading.dto.spotprice;

import java.util.List;

public record SpotPriceFailureResponse(
    String code,
    String message,
    List<SpotPriceAttemptResponse> attempts
) {
}
