package com.trading.dto.lookup;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PricePeakResponse(
    UUID id,
    UUID userId,
    UUID assetId,
    String assetSymbol,
    String assetName,
    UUID lastBuyTransactionId,
    BigDecimal peakPrice,
    OffsetDateTime peakTimestamp,
    Boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
