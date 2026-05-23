package com.trading.service.transaction;

import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.AccumulationTradeAssetSummaryResponse;
import com.trading.dto.transaction.CloseAccumulationTradeRequest;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import com.trading.domain.enums.AccumulationTradeStatus;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface AccumulationTradeService {

    Page<AccumulationTradeResponse> list(
        UUID userId,
        int page,
        int size,
        AccumulationTradeStatus status,
        UUID assetId
    );

    List<AccumulationTradeAssetSummaryResponse> summarizeByAsset(
        UUID userId,
        AccumulationTradeStatus status,
        UUID assetId
    );

    AccumulationTradeResponse open(UUID userId, OpenAccumulationTradeRequest request);

    AccumulationTradeResponse close(UUID userId, CloseAccumulationTradeRequest request);
}
