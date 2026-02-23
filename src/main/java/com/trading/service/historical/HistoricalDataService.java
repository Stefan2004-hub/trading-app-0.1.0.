package com.trading.service.historical;

import com.trading.dto.historical.HistoricalAssetRefreshStatusResponse;
import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.dto.historical.HistoricalSyncStartResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface HistoricalDataService {

    Page<HistoricalDataRowResponse> listForUser(UUID userId, int page, int size);

    List<HistoricalAssetRefreshStatusResponse> listAssetsNeedingRefreshToday(UUID userId);

    HistoricalSyncStartResponse startExtremeSync(UUID userId);

    HistoricalDataSyncResponse syncIncremental(UUID userId, UUID assetId);

    HistoricalDataSyncResponse cleanAndReset(UUID userId);
}
