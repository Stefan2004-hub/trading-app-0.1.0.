package com.trading.service.lookup;

import com.trading.dto.lookup.ExchangeLookupResponse;
import com.trading.dto.lookup.UpsertExchangeRequest;

import java.util.List;
import java.util.UUID;

public interface ExchangeService {

    List<ExchangeLookupResponse> list(String search);

    ExchangeLookupResponse get(UUID id);

    ExchangeLookupResponse create(UpsertExchangeRequest request);

    ExchangeLookupResponse update(UUID id, UpsertExchangeRequest request);

    void delete(UUID id);
}
