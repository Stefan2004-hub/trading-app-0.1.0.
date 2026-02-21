package com.trading.domain.repository;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.enums.AccumulationTradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccumulationTradeRepository extends JpaRepository<AccumulationTrade, UUID> {

    List<AccumulationTrade> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<AccumulationTrade> findAllByUser_IdAndStatusOrderByCreatedAtDesc(
        UUID userId,
        AccumulationTradeStatus status
    );

    Optional<AccumulationTrade> findByIdAndUser_Id(UUID accumulationTradeId, UUID userId);

    Optional<AccumulationTrade> findByUser_IdAndExitTransaction_Id(UUID userId, UUID exitTransactionId);

    List<AccumulationTrade> findAllByUser_IdAndExitTransaction_Id(UUID userId, UUID exitTransactionId);

    List<AccumulationTrade> findAllByUser_IdAndReentryTransaction_Id(UUID userId, UUID reentryTransactionId);

    void deleteAllByUser_IdAndExitTransaction_Id(UUID userId, UUID exitTransactionId);

    void deleteAllByUser_IdAndReentryTransaction_Id(UUID userId, UUID reentryTransactionId);
}
