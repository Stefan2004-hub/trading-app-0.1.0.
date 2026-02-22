package com.trading.dto.historical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HistoricalDataRowResponse(
    UUID id,
    UUID assetId,
    String assetSymbol,
    String assetName,
    LocalDate dayDate,
    BigDecimal highPrice,
    BigDecimal lowPrice,
    BigDecimal closingPrice
) {
}
