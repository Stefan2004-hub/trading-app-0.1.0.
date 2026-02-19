package com.trading.service.transaction;

import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.dto.transaction.UpdateTransactionRequest;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    List<TransactionResponse> list(UUID userId);

    TransactionResponse buy(UUID userId, BuyTransactionRequest request);

    TransactionResponse sell(UUID userId, SellTransactionRequest request);

    TransactionResponse updateTransaction(UUID userId, UUID transactionId, UpdateTransactionRequest request);

    void deleteTransaction(UUID userId, UUID transactionId);
}
