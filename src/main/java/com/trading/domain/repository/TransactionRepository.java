package com.trading.domain.repository;

import com.trading.domain.entity.Transaction;
import com.trading.domain.projection.SellOpportunityProjection;
import com.trading.domain.projection.UserPortfolioPerformanceProjection;
import com.trading.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByUser_IdOrderByTransactionDateDesc(UUID userId);

    List<Transaction> findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(UUID userId, UUID assetId);

    List<Transaction> findAllByUser_IdAndAsset_IdAndTransactionTypeOrderByTransactionDateDesc(
        UUID userId,
        UUID assetId,
        TransactionType transactionType
    );

    Optional<Transaction> findByIdAndUser_Id(UUID transactionId, UUID userId);

    @Query(
        value = """
            SELECT
                upp.user_id AS userId,
                upp.symbol AS symbol,
                upp.exchange AS exchange,
                upp.current_balance AS currentBalance,
                upp.total_invested_usd AS totalInvestedUsd,
                upp.avg_buy_price AS avgBuyPrice
            FROM user_portfolio_performance upp
            WHERE upp.user_id = :userId
            """,
        nativeQuery = true
    )
    List<UserPortfolioPerformanceProjection> findPortfolioPerformanceByUserId(@Param("userId") UUID userId);

    @Query(
        value = """
            SELECT
                so.user_id AS userId,
                so.transaction_id AS transactionId,
                so.symbol AS symbol,
                so.asset_name AS assetName,
                so.transaction_type AS transactionType,
                so.buy_price AS buyPrice,
                so.coin_amount AS coinAmount,
                so.threshold_percent AS thresholdPercent,
                so.target_sell_price AS targetSellPrice
            FROM sell_opportunities so
            WHERE so.user_id = :userId
            """,
        nativeQuery = true
    )
    List<SellOpportunityProjection> findSellOpportunitiesByUserId(@Param("userId") UUID userId);
}
