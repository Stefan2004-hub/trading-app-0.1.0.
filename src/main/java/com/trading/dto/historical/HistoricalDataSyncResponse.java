package com.trading.dto.historical;

import java.util.List;

public record HistoricalDataSyncResponse(
    int assetsProcessed,
    int rowsInserted,
    int assetsSkipped,
    List<SkippedAssetSyncItem> skippedAssets
) {
}
