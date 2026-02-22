package com.trading.service.historical;

import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;

import java.util.List;
import java.util.UUID;

public interface HistoricalDataService {

    List<HistoricalDataRowResponse> listForUser(UUID userId);

    HistoricalDataSyncResponse syncIncremental(UUID userId);

    HistoricalDataSyncResponse cleanAndReset(UUID userId);
}
