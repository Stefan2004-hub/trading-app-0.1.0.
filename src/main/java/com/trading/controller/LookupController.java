package com.trading.controller;

import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.ExchangeLookupResponse;
import com.trading.service.lookup.LookupService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LookupController {

    private final ObjectProvider<LookupService> lookupServiceProvider;

    public LookupController(ObjectProvider<LookupService> lookupServiceProvider) {
        this.lookupServiceProvider = lookupServiceProvider;
    }

    @GetMapping("/assets")
    public ResponseEntity<List<AssetLookupResponse>> assets() {
        LookupService lookupService = lookupServiceProvider.getIfAvailable();
        if (lookupService == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(lookupService.listAssets());
    }

    @GetMapping("/exchanges")
    public ResponseEntity<List<ExchangeLookupResponse>> exchanges() {
        LookupService lookupService = lookupServiceProvider.getIfAvailable();
        if (lookupService == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(lookupService.listExchanges());
    }
}
