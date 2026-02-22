package com.trading.service.marketalert;

import com.trading.dto.marketalert.MarketAlertResponse;
import com.trading.dto.marketalert.MarketScanResponse;

import java.util.List;
import java.util.UUID;

public interface MarketAlertService {

    List<MarketAlertResponse> list(UUID userId);

    MarketScanResponse scan(UUID userId);

    MarketAlertResponse markViewed(UUID userId, UUID alertId);

    void clear(UUID userId);
}
