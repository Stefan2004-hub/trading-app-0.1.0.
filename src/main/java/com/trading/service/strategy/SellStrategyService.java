package com.trading.service.strategy;

import com.trading.dto.strategy.SellStrategyResponse;
import com.trading.dto.strategy.UpsertSellStrategyRequest;

import java.util.List;
import java.util.UUID;

public interface SellStrategyService {

    List<SellStrategyResponse> list(UUID userId);

    SellStrategyResponse upsert(UUID userId, UpsertSellStrategyRequest request);

    void delete(UUID userId, UUID strategyId);
}
