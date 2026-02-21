package com.trading.service.lookup;

import com.trading.dto.lookup.PricePeakResponse;
import com.trading.dto.lookup.UpdatePricePeakRequest;

import java.util.List;
import java.util.UUID;

public interface PricePeakService {

    List<PricePeakResponse> list(UUID userId, String search);

    PricePeakResponse update(UUID userId, UUID pricePeakId, UpdatePricePeakRequest request);

    void delete(UUID userId, UUID pricePeakId);
}
