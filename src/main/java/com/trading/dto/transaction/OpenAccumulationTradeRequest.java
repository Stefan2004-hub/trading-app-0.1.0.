package com.trading.dto.transaction;

import java.util.UUID;

public record OpenAccumulationTradeRequest(
    UUID exitTransactionId,
    String predictionNotes
) {
}
