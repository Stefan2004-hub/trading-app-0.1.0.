package com.trading.dto.marketalert;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketSnapshotDTO(
    String assetName,
    String symbol,
    BigDecimal currentRsi,
    BigDecimal currentStoch,
    LocalDate lastUpdatedDate,
    String momentum
) {
}
