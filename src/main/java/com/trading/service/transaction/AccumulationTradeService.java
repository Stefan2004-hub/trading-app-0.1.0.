package com.trading.service.transaction;

import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.CloseAccumulationTradeRequest;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import com.trading.domain.enums.AccumulationTradeStatus;

import java.util.List;
import java.util.UUID;

public interface AccumulationTradeService {

    List<AccumulationTradeResponse> list(UUID userId, AccumulationTradeStatus status);

    AccumulationTradeResponse open(UUID userId, OpenAccumulationTradeRequest request);

    AccumulationTradeResponse close(UUID userId, CloseAccumulationTradeRequest request);
}
