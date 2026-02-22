package com.trading.controller;

import com.trading.dto.historical.HistoricalDataRowResponse;
import com.trading.dto.historical.HistoricalDataSyncResponse;
import com.trading.security.CurrentUserProvider;
import com.trading.service.historical.HistoricalDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
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
    public ResponseEntity<List<HistoricalDataRowResponse>> list() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(historicalDataService.listForUser(userId));
    }

    @PostMapping("/refresh")
    public ResponseEntity<HistoricalDataSyncResponse> refresh() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(historicalDataService.syncIncremental(userId));
    }

    @PostMapping("/clean-reset")
    public ResponseEntity<HistoricalDataSyncResponse> cleanAndReset() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(historicalDataService.cleanAndReset(userId));
    }
}
