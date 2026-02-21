package com.trading.controller;

import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.CloseAccumulationTradeRequest;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import com.trading.security.CurrentUserProvider;
import com.trading.service.transaction.AccumulationTradeService;
import jakarta.validation.Valid;
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

    private final AccumulationTradeService accumulationTradeService;
    private final CurrentUserProvider currentUserProvider;

    public AccumulationTradeController(
        AccumulationTradeService accumulationTradeService,
        CurrentUserProvider currentUserProvider
    ) {
        this.accumulationTradeService = accumulationTradeService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<List<AccumulationTradeResponse>> list(
        @RequestParam(name = "status", required = false) AccumulationTradeStatus status,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.ok(accumulationTradeService.list(resolvedUserId, status));
    }

    @PostMapping("/open")
    public ResponseEntity<AccumulationTradeResponse> open(
        @Valid @RequestBody OpenAccumulationTradeRequest request,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(accumulationTradeService.open(resolvedUserId, request));
    }

    @PostMapping("/close")
    public ResponseEntity<AccumulationTradeResponse> close(
        @Valid @RequestBody CloseAccumulationTradeRequest request,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.ok(accumulationTradeService.close(resolvedUserId, request));
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

}
