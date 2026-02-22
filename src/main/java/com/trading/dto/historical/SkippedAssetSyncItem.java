package com.trading.dto.historical;

import java.util.UUID;

public record SkippedAssetSyncItem(
    UUID assetId,
    String assetSymbol,
    String reason
) {
}
