package com.trading.service.transaction;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Transaction;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@ConditionalOnBean({AccumulationTradeRepository.class, TransactionRepository.class})
public class AccumulationTradeServiceImpl implements AccumulationTradeService {

    private final AccumulationTradeRepository accumulationTradeRepository;
    private final TransactionRepository transactionRepository;

    public AccumulationTradeServiceImpl(
        AccumulationTradeRepository accumulationTradeRepository,
        TransactionRepository transactionRepository
    ) {
        this.accumulationTradeRepository = accumulationTradeRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public AccumulationTradeResponse open(UUID userId, OpenAccumulationTradeRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.exitTransactionId(), "exitTransactionId is required");

        Transaction exitTransaction = transactionRepository.findByIdAndUser_Id(request.exitTransactionId(), userId)
            .orElseThrow(() -> new IllegalArgumentException("Exit transaction not found for user"));

        if (exitTransaction.getTransactionType() != TransactionType.SELL) {
            throw new IllegalArgumentException("Exit transaction must be SELL");
        }

        accumulationTradeRepository.findByUser_IdAndExitTransaction_Id(userId, request.exitTransactionId())
            .ifPresent(existing -> {
                throw new IllegalArgumentException("Accumulation trade already exists for this exit transaction");
            });

        AccumulationTrade trade = new AccumulationTrade();
        trade.setUser(exitTransaction.getUser());
        trade.setAsset(exitTransaction.getAsset());
        trade.setExitTransaction(exitTransaction);
        trade.setReentryTransaction(null);
        trade.setOldCoinAmount(exitTransaction.getGrossAmount());
        trade.setNewCoinAmount(null);
        trade.setStatus(AccumulationTradeStatus.OPEN);
        trade.setExitPriceUsd(exitTransaction.getUnitPriceUsd());
        trade.setReentryPriceUsd(null);
        trade.setCreatedAt(OffsetDateTime.now());
        trade.setClosedAt(null);
        trade.setPredictionNotes(request.predictionNotes());

        AccumulationTrade saved = accumulationTradeRepository.save(trade);
        return new AccumulationTradeResponse(
            saved.getId(),
            saved.getUser().getId(),
            saved.getAsset().getId(),
            saved.getExitTransaction().getId(),
            saved.getReentryTransaction() == null ? null : saved.getReentryTransaction().getId(),
            saved.getOldCoinAmount(),
            saved.getNewCoinAmount(),
            saved.getAccumulationDelta(),
            saved.getStatus(),
            saved.getExitPriceUsd(),
            saved.getReentryPriceUsd(),
            saved.getCreatedAt(),
            saved.getClosedAt(),
            saved.getPredictionNotes()
        );
    }
}
