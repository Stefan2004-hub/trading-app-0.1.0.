package com.trading.dto.historical;

import java.time.OffsetDateTime;

public record HistoricalSyncStartResponse(
    String status,
    String message,
    OffsetDateTime startedAt
) {
}
