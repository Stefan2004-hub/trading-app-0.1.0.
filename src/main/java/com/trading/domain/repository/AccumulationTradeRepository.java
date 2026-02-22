package com.trading.domain.repository;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.enums.AccumulationTradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    @Query(
        """
            SELECT at
            FROM AccumulationTrade at
            WHERE at.user.id = :userId
              AND (
                at.exitTransaction.id IN :transactionIds
                OR at.reentryTransaction.id IN :transactionIds
              )
            """
    )
    List<AccumulationTrade> findAllLinkedToTransactions(
        @Param("userId") UUID userId,
        @Param("transactionIds") Collection<UUID> transactionIds
    );

    void deleteAllByUser_IdAndExitTransaction_Id(UUID userId, UUID exitTransactionId);

    void deleteAllByUser_IdAndReentryTransaction_Id(UUID userId, UUID reentryTransactionId);
}
