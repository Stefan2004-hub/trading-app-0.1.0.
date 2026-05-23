package com.trading.controller;

import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.dto.transaction.AccumulationTradeAssetSummaryResponse;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.CloseAccumulationTradeRequest;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import com.trading.security.CurrentUserProvider;
import com.trading.service.transaction.AccumulationTradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<Page<AccumulationTradeResponse>> list(
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
        @RequestParam(name = "assetId", required = false) UUID assetId,
        @RequestParam(name = "status", required = false) AccumulationTradeStatus status,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.ok(accumulationTradeService.list(resolvedUserId, page, size, status, assetId));
    }

    @GetMapping("/grouped-by-asset")
    public ResponseEntity<List<AccumulationTradeAssetSummaryResponse>> groupedByAsset(
        @RequestParam(name = "assetId", required = false) UUID assetId,
        @RequestParam(name = "status", required = false) AccumulationTradeStatus status,
        @RequestParam(name = "userId", required = false) UUID userId
    ) {
        UUID resolvedUserId = resolveUserId(userId);
        return ResponseEntity.ok(accumulationTradeService.summarizeByAsset(resolvedUserId, status, assetId));
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
