package com.trading.service.marketalert;

import com.trading.dto.marketalert.MarketAlertResponse;
import com.trading.dto.marketalert.MarketScanResponse;
import com.trading.dto.marketalert.MarketSnapshotDTO;

import java.util.List;
import java.util.UUID;

public interface MarketAlertService {

    List<MarketAlertResponse> list(UUID userId);

    List<MarketSnapshotDTO> listTechnicalSummary();

    MarketScanResponse scan(UUID userId);

    MarketAlertResponse markViewed(UUID userId, UUID alertId);

    void clear(UUID userId);
}
