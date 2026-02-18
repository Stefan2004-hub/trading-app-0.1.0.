DROP VIEW IF EXISTS user_portfolio_performance;

CREATE VIEW user_portfolio_performance AS
SELECT
    t.user_id,
    a.symbol,
    e.name AS exchange,
    SUM(
        CASE
            WHEN t.transaction_type = 'BUY' THEN t.net_amount
            ELSE -t.net_amount
        END
    ) AS current_balance,
    SUM(
        CASE
            WHEN t.transaction_type = 'BUY' THEN t.total_spent_usd
            ELSE -(t.total_spent_usd - COALESCE(t.realized_pnl, 0))
        END
    ) AS total_invested_usd,
    CASE
        WHEN SUM(
            CASE
                WHEN t.transaction_type = 'BUY' THEN t.net_amount
                ELSE -t.net_amount
            END
        ) > 0
        THEN SUM(
            CASE
                WHEN t.transaction_type = 'BUY' THEN t.total_spent_usd
                ELSE -(t.total_spent_usd - COALESCE(t.realized_pnl, 0))
            END
        ) /
        SUM(
            CASE
                WHEN t.transaction_type = 'BUY' THEN t.net_amount
                ELSE -t.net_amount
            END
        )
        ELSE 0
    END AS avg_buy_price
FROM transactions t
JOIN assets a ON t.asset_id = a.id
JOIN exchanges e ON t.exchange_id = e.id
GROUP BY t.user_id, a.symbol, e.name;
