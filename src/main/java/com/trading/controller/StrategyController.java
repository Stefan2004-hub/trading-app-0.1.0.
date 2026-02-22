package com.trading.controller;

import com.trading.dto.strategy.BuyStrategyResponse;
import com.trading.dto.strategy.GenerateStrategyAlertsRequest;
import com.trading.dto.strategy.SellStrategyResponse;
import com.trading.dto.strategy.StrategyAlertResponse;
import com.trading.dto.strategy.UpsertBuyStrategyRequest;
import com.trading.dto.strategy.UpsertSellStrategyRequest;
import com.trading.security.CurrentUserProvider;
import com.trading.service.strategy.BuyStrategyService;
import com.trading.service.strategy.SellStrategyService;
import com.trading.service.strategy.StrategyAlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final SellStrategyService sellStrategyService;
    private final BuyStrategyService buyStrategyService;
    private final StrategyAlertService strategyAlertService;
    private final CurrentUserProvider currentUserProvider;

    public StrategyController(
        SellStrategyService sellStrategyService,
        BuyStrategyService buyStrategyService,
        StrategyAlertService strategyAlertService,
        CurrentUserProvider currentUserProvider
    ) {
        this.sellStrategyService = sellStrategyService;
        this.buyStrategyService = buyStrategyService;
        this.strategyAlertService = strategyAlertService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/sell")
    public ResponseEntity<List<SellStrategyResponse>> listSellStrategies() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(sellStrategyService.list(userId));
    }

    @PostMapping("/sell")
    public ResponseEntity<SellStrategyResponse> upsertSellStrategy(@Valid @RequestBody UpsertSellStrategyRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(sellStrategyService.upsert(userId, request));
    }

    @DeleteMapping("/sell/{id}")
    public ResponseEntity<Void> deleteSellStrategy(@PathVariable("id") UUID strategyId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        sellStrategyService.delete(userId, strategyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buy")
    public ResponseEntity<List<BuyStrategyResponse>> listBuyStrategies() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(buyStrategyService.list(userId));
    }

    @PostMapping("/buy")
    public ResponseEntity<BuyStrategyResponse> upsertBuyStrategy(@Valid @RequestBody UpsertBuyStrategyRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(buyStrategyService.upsert(userId, request));
    }

    @DeleteMapping("/buy/{id}")
    public ResponseEntity<Void> deleteBuyStrategy(@PathVariable("id") UUID strategyId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        buyStrategyService.delete(userId, strategyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<StrategyAlertResponse>> listAlerts() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(strategyAlertService.list(userId));
    }

    @PostMapping("/alerts/generate")
    public ResponseEntity<List<StrategyAlertResponse>> generateAlerts(@Valid @RequestBody GenerateStrategyAlertsRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(strategyAlertService.generate(userId, request));
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public ResponseEntity<StrategyAlertResponse> acknowledgeAlert(@PathVariable("id") UUID alertId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(strategyAlertService.acknowledge(userId, alertId));
    }

    @DeleteMapping("/alerts/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable("id") UUID alertId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        strategyAlertService.delete(userId, alertId);
        return ResponseEntity.noContent().build();
    }
}
