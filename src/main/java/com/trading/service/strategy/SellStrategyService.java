package com.trading.service.strategy;

import com.trading.dto.strategy.SellStrategyResponse;
import com.trading.dto.strategy.UpsertSellStrategyRequest;

import java.util.UUID;

public interface SellStrategyService {

    SellStrategyResponse upsert(UUID userId, UpsertSellStrategyRequest request);
}
