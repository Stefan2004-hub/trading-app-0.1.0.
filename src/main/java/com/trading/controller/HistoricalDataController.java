package com.trading.controller;

import com.trading.dto.historical.HistoricalAssetRefreshStatusResponse;
import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.security.CurrentUserProvider;
import com.trading.service.historical.HistoricalDataService;
import jakarta.validation.constraints.Min;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;
import java.util.List;

@RestController
@Validated
@ConditionalOnProperty(name = "app.historical.enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/historical-data")
public class HistoricalDataController {

    private final HistoricalDataService historicalDataService;
    private final CurrentUserProvider currentUserProvider;

    public HistoricalDataController(
        HistoricalDataService historicalDataService,
        CurrentUserProvider currentUserProvider
    ) {
        this.historicalDataService = historicalDataService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<Page<HistoricalDataRowResponse>> list(
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) int size
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(historicalDataService.listForUser(userId, page, size));
    }

    @GetMapping("/missing-today")
    public ResponseEntity<List<HistoricalAssetRefreshStatusResponse>> listMissingToday() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(historicalDataService.listAssetsNeedingRefreshToday(userId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<HistoricalDataSyncResponse> refresh(
        @RequestParam(name = "assetId", required = false) UUID assetId
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(historicalDataService.syncIncremental(userId, assetId));
    }

    @PostMapping("/clean-reset")
    public ResponseEntity<HistoricalDataSyncResponse> cleanAndReset() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(historicalDataService.cleanAndReset(userId));
    }
}
