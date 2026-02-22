package com.trading.service.strategy;

import com.trading.dto.strategy.BuyStrategyResponse;
import com.trading.dto.strategy.UpsertBuyStrategyRequest;

import java.util.List;
import java.util.UUID;

public interface BuyStrategyService {

    List<BuyStrategyResponse> list(UUID userId);

    BuyStrategyResponse upsert(UUID userId, UpsertBuyStrategyRequest request);

    void delete(UUID userId, UUID strategyId);
}
