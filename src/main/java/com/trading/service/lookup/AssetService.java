package com.trading.service.lookup;

import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.UpsertAssetRequest;

import java.util.List;
import java.util.UUID;

public interface AssetService {

    List<AssetLookupResponse> list(String search);

    AssetLookupResponse get(UUID id);

    AssetLookupResponse create(UpsertAssetRequest request);

    AssetLookupResponse update(UUID id, UpsertAssetRequest request);

    void delete(UUID id);
}
