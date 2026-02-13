package com.trading.controller;

import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.security.CurrentUserProvider;
import com.trading.service.transaction.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
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
    public ResponseEntity<List<TransactionResponse>> list() {
        UUID userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(transactionService.list(userId));
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
}
