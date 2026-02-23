package com.trading.controller;

import com.trading.dto.marketalert.MarketAlertResponse;
import com.trading.dto.marketalert.MarketScanResponse;
import com.trading.dto.marketalert.MarketSnapshotDTO;
import com.trading.security.CurrentUserProvider;
import com.trading.service.marketalert.MarketAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/market-alerts")
public class MarketAlertController {

    private final MarketAlertService marketAlertService;
    private final CurrentUserProvider currentUserProvider;

    public MarketAlertController(
        MarketAlertService marketAlertService,
        CurrentUserProvider currentUserProvider
    ) {
        this.marketAlertService = marketAlertService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<MarketAlertResponse>> list() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(marketAlertService.list(userId));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<MarketSnapshotDTO>> listSummary() {
        return ResponseEntity.ok(marketAlertService.listTechnicalSummary());
    }

    @PostMapping("/scan")
    public ResponseEntity<MarketScanResponse> scan() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(marketAlertService.scan(userId));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<MarketAlertResponse> markViewed(@PathVariable("id") UUID alertId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(marketAlertService.markViewed(userId, alertId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearHistory() {
        UUID userId = currentUserProvider.getCurrentUserId();
        marketAlertService.clear(userId);
        return ResponseEntity.noContent().build();
    }
}
