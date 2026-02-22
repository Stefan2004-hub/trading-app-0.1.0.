package com.trading.dto.historical;

import java.time.LocalDate;
import java.util.UUID;

public record HistoricalAssetRefreshStatusResponse(
    UUID assetId,
    String assetSymbol,
    String assetName,
    LocalDate missingDate
) {
}
