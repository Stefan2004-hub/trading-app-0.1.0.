package com.trading.service.transaction;

import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.SellTransactionRequest;
import com.trading.dto.transaction.UpdateTransactionNetAmountRequest;
import com.trading.dto.transaction.TransactionResponse;
import com.trading.dto.transaction.UpdateTransactionRequest;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface TransactionService {

    Page<TransactionResponse> list(UUID userId, int page, int size, String search);

    TransactionResponse buy(UUID userId, BuyTransactionRequest request);

    TransactionResponse sell(UUID userId, SellTransactionRequest request);

    TransactionResponse updateTransaction(UUID userId, UUID transactionId, UpdateTransactionRequest request);

    TransactionResponse updateTransactionNetAmount(UUID userId, UUID transactionId, UpdateTransactionNetAmountRequest request);

    void deleteTransaction(UUID userId, UUID transactionId);
}
