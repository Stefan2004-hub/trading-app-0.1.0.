package com.trading.domain.repository;

import com.trading.domain.entity.Transaction;
import com.trading.domain.projection.UserAssetLatestPriceProjection;
import com.trading.domain.projection.UserAssetRealizedPnlProjection;
import com.trading.domain.projection.UserAssetSummaryProjection;
import com.trading.domain.projection.SellOpportunityProjection;
import com.trading.domain.projection.UserPortfolioPerformanceProjection;
import com.trading.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findAllByUser_IdOrderByTransactionDateDesc(UUID userId);

    @Query(
        """
            SELECT t
            FROM Transaction t
            JOIN t.asset a
            JOIN t.exchange e
            WHERE t.user.id = :userId
              AND (
                :searchPattern IS NULL
                OR a.symbol LIKE :searchPattern
                OR a.name LIKE :searchPattern
                OR e.symbol LIKE :searchPattern
                OR e.name LIKE :searchPattern
              )
            """
    )
    Page<Transaction> findByUser_IdAndSearch(
        @Param("userId") UUID userId,
        @Param("searchPattern") String searchPattern,
        Pageable pageable
    );

    List<Transaction> findAllByUser_IdAndAsset_IdOrderByTransactionDateDesc(UUID userId, UUID assetId);

    List<Transaction> findAllByUser_IdAndAsset_IdAndTransactionTypeOrderByTransactionDateDesc(
        UUID userId,
        UUID assetId,
        TransactionType transactionType
    );

    List<Transaction> findAllByUser_IdAndIdIn(UUID userId, Collection<UUID> ids);

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
            SELECT DISTINCT ON (a.symbol, e.name)
                a.symbol AS symbol,
                e.name AS exchange,
                t.unit_price_usd AS latestUnitPriceUsd
            FROM transactions t
            JOIN assets a ON a.id = t.asset_id
            JOIN exchanges e ON e.id = t.exchange_id
            WHERE t.user_id = :userId
            ORDER BY a.symbol, e.name, t.transaction_date DESC, t.id DESC
            """,
        nativeQuery = true
    )
    List<UserAssetLatestPriceProjection> findLatestUnitPricesByUserId(@Param("userId") UUID userId);

    @Query(
        value = """
            SELECT
                a.symbol AS symbol,
                e.name AS exchange,
                COALESCE(SUM(t.realized_pnl), 0) AS realizedPnl
            FROM transactions t
            JOIN assets a ON a.id = t.asset_id
            JOIN exchanges e ON e.id = t.exchange_id
            WHERE t.user_id = :userId
            GROUP BY a.symbol, e.name
            """,
        nativeQuery = true
    )
    List<UserAssetRealizedPnlProjection> findRealizedPnlByUserId(@Param("userId") UUID userId);

    @Query(
        value = """
            SELECT
                a.name AS assetName,
                a.symbol AS assetSymbol,
                SUM(
                    CASE
                        WHEN t.transaction_type = 'BUY' THEN t.net_amount
                        ELSE -t.net_amount
                    END
                ) AS netQuantity,
                SUM(
                    CASE
                        WHEN t.transaction_type = 'BUY' THEN t.total_spent_usd
                        ELSE -(t.total_spent_usd - COALESCE(t.realized_pnl, 0))
                    END
                ) AS totalInvested,
                SUM(
                    CASE
                        WHEN t.transaction_type = 'SELL' THEN COALESCE(t.realized_pnl, 0)
                        ELSE 0
                    END
                ) AS totalRealizedProfit
            FROM transactions t
            JOIN assets a ON a.id = t.asset_id
            WHERE t.user_id = :userId
            GROUP BY a.id, a.name, a.symbol
            HAVING
                SUM(
                    CASE
                        WHEN t.transaction_type = 'BUY' THEN t.net_amount
                        ELSE -t.net_amount
                    END
                ) <> 0
                OR SUM(
                    CASE
                        WHEN t.transaction_type = 'SELL' THEN COALESCE(t.realized_pnl, 0)
                        ELSE 0
                    END
                ) <> 0
            ORDER BY a.symbol
            """,
        nativeQuery = true
    )
    List<UserAssetSummaryProjection> findAssetSummariesByUserId(@Param("userId") UUID userId);

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
