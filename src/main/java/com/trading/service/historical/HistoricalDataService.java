package com.trading.service.historical;

import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface HistoricalDataService {

    Page<HistoricalDataRowResponse> listForUser(UUID userId, int page, int size);

    HistoricalDataSyncResponse syncIncremental(UUID userId, UUID assetId);

    HistoricalDataSyncResponse cleanAndReset(UUID userId);
}
