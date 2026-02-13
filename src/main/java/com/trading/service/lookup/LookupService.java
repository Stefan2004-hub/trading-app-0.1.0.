package com.trading.service.lookup;

import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.ExchangeLookupResponse;

import java.util.List;

public interface LookupService {

    List<AssetLookupResponse> listAssets();

    List<ExchangeLookupResponse> listExchanges();
}
