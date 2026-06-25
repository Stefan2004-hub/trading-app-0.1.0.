package com.trading.domain.repository;

import com.trading.domain.entity.AccumulationTrade;
import com.trading.domain.enums.AccumulationTradeStatus;
import com.trading.domain.projection.AccumulationTradeAssetSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccumulationTradeRepository extends JpaRepository<AccumulationTrade, UUID> {

    List<AccumulationTrade> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);

    Page<AccumulationTrade> findAllByUser_Id(UUID userId, Pageable pageable);

    List<AccumulationTrade> findAllByUser_IdAndStatusOrderByCreatedAtDesc(
        UUID userId,
        AccumulationTradeStatus status
    );

    Page<AccumulationTrade> findAllByUser_IdAndStatus(
        UUID userId,
        AccumulationTradeStatus status,
        Pageable pageable
    );

    Page<AccumulationTrade> findAllByUser_IdAndAsset_Id(
        UUID userId,
        UUID assetId,
        Pageable pageable
    );

    Page<AccumulationTrade> findAllByUser_IdAndStatusAndAsset_Id(
        UUID userId,
        AccumulationTradeStatus status,
        UUID assetId,
        Pageable pageable
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

    @Query(
        """
            SELECT
                at.asset.id AS assetId,
                COALESCE(SUM(COALESCE(at.accumulationDelta, at.newCoinAmount - at.oldCoinAmount)), 0) AS totalAccumulationDelta,
                COUNT(at.id) AS tradeCount
            FROM AccumulationTrade at
            WHERE at.user.id = :userId
              AND at.status = :status
              AND at.newCoinAmount IS NOT NULL
              AND at.oldCoinAmount IS NOT NULL
            GROUP BY at.asset.id
            ORDER BY at.asset.id
            """
    )
    List<AccumulationTradeAssetSummaryProjection> summarizeByAssetAndStatus(
        @Param("userId") UUID userId,
        @Param("status") AccumulationTradeStatus status
    );

    @Query(
        """
            SELECT
                at.asset.id AS assetId,
                COALESCE(SUM(COALESCE(at.accumulationDelta, at.newCoinAmount - at.oldCoinAmount)), 0) AS totalAccumulationDelta,
                COUNT(at.id) AS tradeCount
            FROM AccumulationTrade at
            WHERE at.user.id = :userId
              AND at.status = :status
              AND at.asset.id = :assetId
              AND at.newCoinAmount IS NOT NULL
              AND at.oldCoinAmount IS NOT NULL
            GROUP BY at.asset.id
            ORDER BY at.asset.id
            """
    )
    List<AccumulationTradeAssetSummaryProjection> summarizeByAssetAndStatusAndAssetId(
        @Param("userId") UUID userId,
        @Param("status") AccumulationTradeStatus status,
        @Param("assetId") UUID assetId
    );

    void deleteAllByUser_IdAndExitTransaction_Id(UUID userId, UUID exitTransactionId);

    void deleteAllByUser_IdAndReentryTransaction_Id(UUID userId, UUID reentryTransactionId);
}
