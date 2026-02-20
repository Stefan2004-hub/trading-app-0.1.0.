package com.trading.controller;

import com.trading.dto.lookup.AssetLookupResponse;
import com.trading.dto.lookup.ExchangeLookupResponse;
import com.trading.dto.lookup.UpsertAssetRequest;
import com.trading.dto.lookup.UpsertExchangeRequest;
import com.trading.service.lookup.AssetService;
import com.trading.service.lookup.ExchangeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LookupController {

    private final AssetService assetService;
    private final ExchangeService exchangeService;

    public LookupController(AssetService assetService, ExchangeService exchangeService) {
        this.assetService = assetService;
        this.exchangeService = exchangeService;
    }

    @GetMapping("/assets")
    public ResponseEntity<List<AssetLookupResponse>> listAssets(
        @RequestParam(name = "search", required = false) String search
    ) {
        return ResponseEntity.ok(assetService.list(search));
    }

    @GetMapping("/assets/{assetId}")
    public ResponseEntity<AssetLookupResponse> getAsset(@PathVariable UUID assetId) {
        return ResponseEntity.ok(assetService.get(assetId));
    }

    @PostMapping("/assets")
    public ResponseEntity<AssetLookupResponse> createAsset(@Valid @RequestBody UpsertAssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
    }

    @PutMapping("/assets/{assetId}")
    public ResponseEntity<AssetLookupResponse> updateAsset(
        @PathVariable UUID assetId,
        @Valid @RequestBody UpsertAssetRequest request
    ) {
        return ResponseEntity.ok(assetService.update(assetId, request));
    }

    @DeleteMapping("/assets/{assetId}")
    public ResponseEntity<Void> deleteAsset(@PathVariable UUID assetId) {
        assetService.delete(assetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exchanges")
    public ResponseEntity<List<ExchangeLookupResponse>> listExchanges(
        @RequestParam(name = "search", required = false) String search
    ) {
        return ResponseEntity.ok(exchangeService.list(search));
    }

    @GetMapping("/exchanges/{exchangeId}")
    public ResponseEntity<ExchangeLookupResponse> getExchange(@PathVariable UUID exchangeId) {
        return ResponseEntity.ok(exchangeService.get(exchangeId));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<ExchangeLookupResponse> createExchange(@Valid @RequestBody UpsertExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeService.create(request));
    }

    @PutMapping("/exchanges/{exchangeId}")
    public ResponseEntity<ExchangeLookupResponse> updateExchange(
        @PathVariable UUID exchangeId,
        @Valid @RequestBody UpsertExchangeRequest request
    ) {
        return ResponseEntity.ok(exchangeService.update(exchangeId, request));
    }

    @DeleteMapping("/exchanges/{exchangeId}")
    public ResponseEntity<Void> deleteExchange(@PathVariable UUID exchangeId) {
        exchangeService.delete(exchangeId);
        return ResponseEntity.noContent().build();
    }
}
