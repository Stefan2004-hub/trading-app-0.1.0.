package com.trading.controller;

import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.ExchangeLookupResponse;
import com.trading.service.lookup.LookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LookupController {

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/assets")
    public ResponseEntity<List<AssetLookupResponse>> assets() {
        return ResponseEntity.ok(lookupService.listAssets());
    }

    @GetMapping("/exchanges")
    public ResponseEntity<List<ExchangeLookupResponse>> exchanges() {
        return ResponseEntity.ok(lookupService.listExchanges());
    }
}
