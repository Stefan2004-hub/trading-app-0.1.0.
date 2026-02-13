package com.trading.service.transaction;

import com.trading.dto.transaction.BuyTransactionRequest;
import com.trading.dto.transaction.TransactionResponse;

import java.util.UUID;

public interface TransactionService {

    TransactionResponse buy(UUID userId, BuyTransactionRequest request);
}
