package com.trading.controller;

import com.trading.domain.enums.TransactionListView;
import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.dto.transaction.UpdateTransactionNetAmountRequest;
import com.trading.dto.transaction.UpdateTransactionRequest;
import com.trading.security.CurrentUserProvider;
import com.trading.service.transaction.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CurrentUserProvider currentUserProvider;

    public TransactionController(
        TransactionService transactionService,
        CurrentUserProvider currentUserProvider
    ) {
        this.transactionService = transactionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> list(
        @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
        @RequestParam(name = "size", defaultValue = "20") @Min(1) int size,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "view", defaultValue = "OPEN") TransactionListView view,
        @RequestParam(name = "groupSize", required = false) @Min(1) Integer groupSize
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        int resolvedGroupSize = groupSize == null ? size : groupSize;
        return ResponseEntity.ok(transactionService.list(userId, page, size, search, view, resolvedGroupSize));
    }

    @PostMapping("/buy")
    public ResponseEntity<TransactionResponse> buy(@Valid @RequestBody BuyTransactionRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.buy(userId, request));
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionResponse> sell(@Valid @RequestBody SellTransactionRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.sell(userId, request));
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> update(
        @PathVariable UUID transactionId,
        @RequestBody UpdateTransactionRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(transactionService.updateTransaction(userId, transactionId, request));
    }

    @PatchMapping("/{transactionId}/net-amount")
    public ResponseEntity<TransactionResponse> updateNetAmount(
        @PathVariable UUID transactionId,
        @Valid @RequestBody UpdateTransactionNetAmountRequest request
    ) {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(transactionService.updateTransactionNetAmount(userId, transactionId, request));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(@PathVariable UUID transactionId) {
        UUID userId = currentUserProvider.getCurrentUserId();
        transactionService.deleteTransaction(userId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
