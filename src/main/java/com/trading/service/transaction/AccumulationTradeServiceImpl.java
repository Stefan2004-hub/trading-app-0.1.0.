package com.trading.service.transaction;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.entity.Transaction;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.enums.TransactionType;
import com.trading.domain.repository.AccumulationTradeRepository;
import com.trading.domain.repository.TransactionRepository;
import com.trading.dto.transaction.AccumulationTradeResponse;
import com.trading.dto.transaction.CloseAccumulationTradeRequest;
import com.trading.dto.transaction.OpenAccumulationTradeRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
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
    public List<AccumulationTradeResponse> list(UUID userId, AccumulationTradeStatus status) {
        Objects.requireNonNull(userId, "userId is required");

        List<AccumulationTrade> trades = status == null
            ? accumulationTradeRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
            : accumulationTradeRepository.findAllByUser_IdAndStatusOrderByCreatedAtDesc(userId, status);

        return trades.stream().map(AccumulationTradeServiceImpl::toResponse).toList();
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
        return toResponse(saved);
    }

    @Override
    public AccumulationTradeResponse close(UUID userId, CloseAccumulationTradeRequest request) {
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(request, "request is required");
        Objects.requireNonNull(request.accumulationTradeId(), "accumulationTradeId is required");
        Objects.requireNonNull(request.reentryTransactionId(), "reentryTransactionId is required");

        AccumulationTrade trade = accumulationTradeRepository.findByIdAndUser_Id(request.accumulationTradeId(), userId)
            .orElseThrow(() -> new IllegalArgumentException("Accumulation trade not found for user"));

        if (trade.getStatus() != AccumulationTradeStatus.OPEN) {
            throw new IllegalArgumentException("Only OPEN accumulation trades can be closed");
        }

        Transaction reentryTransaction = transactionRepository.findByIdAndUser_Id(request.reentryTransactionId(), userId)
            .orElseThrow(() -> new IllegalArgumentException("Reentry transaction not found for user"));

        if (reentryTransaction.getTransactionType() != TransactionType.BUY) {
            throw new IllegalArgumentException("Reentry transaction must be BUY");
        }
        if (!trade.getAsset().getId().equals(reentryTransaction.getAsset().getId())) {
            throw new IllegalArgumentException("Reentry transaction asset must match accumulation trade asset");
        }

        trade.setReentryTransaction(reentryTransaction);
        trade.setNewCoinAmount(reentryTransaction.getNetAmount());
        trade.setReentryPriceUsd(reentryTransaction.getUnitPriceUsd());
        trade.setStatus(AccumulationTradeStatus.CLOSED);
        trade.setClosedAt(OffsetDateTime.now());
        if (request.predictionNotes() != null) {
            trade.setPredictionNotes(request.predictionNotes());
        }

        AccumulationTrade saved = accumulationTradeRepository.save(trade);
        return toResponse(saved);
    }

    private static AccumulationTradeResponse toResponse(AccumulationTrade trade) {
        BigDecimal derivedDelta = trade.getAccumulationDelta();
        if (derivedDelta == null && trade.getOldCoinAmount() != null && trade.getNewCoinAmount() != null) {
            derivedDelta = trade.getNewCoinAmount().subtract(trade.getOldCoinAmount());
        }
        return new AccumulationTradeResponse(
            trade.getId(),
            trade.getUser().getId(),
            trade.getAsset().getId(),
            trade.getExitTransaction().getId(),
            trade.getReentryTransaction() == null ? null : trade.getReentryTransaction().getId(),
            trade.getOldCoinAmount(),
            trade.getNewCoinAmount(),
            derivedDelta,
            trade.getStatus(),
            trade.getExitPriceUsd(),
            trade.getReentryPriceUsd(),
            trade.getCreatedAt(),
            trade.getClosedAt(),
            trade.getPredictionNotes()
        );
    }
}
