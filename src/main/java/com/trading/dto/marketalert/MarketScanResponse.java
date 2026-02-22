package com.trading.dto.marketalert;

public record MarketScanResponse(
    int assetsProcessed,
    int alertsCreated,
    int fixedAlertsCreated,
    int dynamicAlertsCreated
) {
}
