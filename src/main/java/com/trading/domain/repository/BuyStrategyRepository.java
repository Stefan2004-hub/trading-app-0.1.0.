package com.trading.domain.repository;

import com.trading.domain.entity.BuyStrategy;
import com.trading.domain.projection.BuyOpportunityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuyStrategyRepository extends JpaRepository<BuyStrategy, UUID> {

    List<BuyStrategy> findAllByUser_IdOrderByUpdatedAtDesc(UUID userId);

    Optional<BuyStrategy> findByUser_IdAndAsset_Id(UUID userId, UUID assetId);

    Optional<BuyStrategy> findByIdAndUser_Id(UUID strategyId, UUID userId);

    @Query(
        value = """
            SELECT
                bo.user_id AS userId,
                bo.asset_id AS assetId,
                bo.symbol AS symbol,
                bo.asset_name AS assetName,
                bo.dip_threshold_percent AS dipThresholdPercent,
                bo.buy_amount_usd AS buyAmountUsd,
                bo.peak_price AS peakPrice,
                bo.target_buy_price AS targetBuyPrice,
                bo.last_buy_transaction_id AS lastBuyTransactionId,
                bo.last_peak_timestamp AS lastPeakTimestamp
            FROM buy_opportunities bo
            WHERE bo.user_id = :userId
            """,
        nativeQuery = true
    )
    List<BuyOpportunityProjection> findBuyOpportunitiesByUserId(@Param("userId") UUID userId);
}
