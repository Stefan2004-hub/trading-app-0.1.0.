package com.trading.service.strategy;

import com.trading.dto.strategy.GenerateStrategyAlertsRequest;
import com.trading.dto.strategy.StrategyAlertResponse;

import java.util.List;
import java.util.UUID;

public interface StrategyAlertService {

    List<StrategyAlertResponse> generate(UUID userId, GenerateStrategyAlertsRequest request);
}
