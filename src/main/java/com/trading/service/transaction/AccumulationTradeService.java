package com.trading.service.transaction;

import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;

import java.util.UUID;

public interface AccumulationTradeService {

    AccumulationTradeResponse open(UUID userId, OpenAccumulationTradeRequest request);
}
