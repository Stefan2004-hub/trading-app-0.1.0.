package com.trading.service.strategy;

import com.trading.dto.strategy.BuyStrategyResponse;
import com.trading.dto.strategy.UpsertBuyStrategyRequest;

import java.util.UUID;

public interface BuyStrategyService {

    BuyStrategyResponse upsert(UUID userId, UpsertBuyStrategyRequest request);
}
