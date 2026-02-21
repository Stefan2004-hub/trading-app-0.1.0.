package com.trading.controller;

import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.CloseAccumulationTradeRequest;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import com.trading.security.CurrentUserProvider;
import com.trading.service.transaction.AccumulationTradeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/accumulation-trades")
public class AccumulationTradeController {

    private final ObjectProvider<AccumulationTradeService> accumulationTradeServiceProvider;
    private final CurrentUserProvider currentUserProvider;

    public AccumulationTradeController(
        ObjectProvider<AccumulationTradeService> accumulationTradeServiceProvider,
        CurrentUserProvider currentUserProvider
    ) {
        this.accumulationTradeServiceProvider = accumulationTradeServiceProvider;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<AccumulationTradeResponse>> list(
        @RequestParam(name = "status", required = false) AccumulationTradeStatus status,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.ok(requireAccumulationTradeService().list(resolvedUserId, status));
    }

    @PostMapping("/open")
    public ResponseEntity<AccumulationTradeResponse> open(
        @Valid @RequestBody OpenAccumulationTradeRequest request,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(requireAccumulationTradeService().open(resolvedUserId, request));
    }

    @PostMapping("/close")
    public ResponseEntity<AccumulationTradeResponse> close(
        @Valid @RequestBody CloseAccumulationTradeRequest request,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.ok(requireAccumulationTradeService().close(resolvedUserId, request));
    }

    private UUID resolveUserId(UUID providedUserId) {
        UUID authenticatedUserId = currentUserProvider.getCurrentUserId();
        if (providedUserId == null) {
            return authenticatedUserId;
        }
        if (!providedUserId.equals(authenticatedUserId)) {
            throw new IllegalArgumentException("Provided userId does not match authenticated user");
        }
        return authenticatedUserId;
    }

    private AccumulationTradeService requireAccumulationTradeService() {
        AccumulationTradeService service = accumulationTradeServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalArgumentException("Accumulation trade feature is not available in this environment");
        }
        return service;
    }

}
