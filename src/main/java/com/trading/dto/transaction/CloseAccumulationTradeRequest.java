package com.trading.dto.transaction;

import java.util.UUID;

public record CloseAccumulationTradeRequest(
    UUID accumulationTradeId,
    UUID reentryTransactionId,
    String predictionNotes
) {
}
