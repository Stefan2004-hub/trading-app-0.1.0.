ALTER TABLE transactions
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);

ALTER TABLE accumulation_trades
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);

ALTER TABLE sell_strategies
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);

ALTER TABLE buy_strategies
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);

ALTER TABLE strategy_alerts
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);

ALTER TABLE price_peaks
    ADD COLUMN user_id UUID NOT NULL REFERENCES users(id);

DROP VIEW IF EXISTS user_portfolio_performance;
CREATE VIEW user_portfolio_performance AS
SELECT
    t.user_id,
    a.symbol,
    e.name AS exchange,
    SUM(CASE WHEN t.transaction_type = 'BUY' THEN t.net_amount ELSE -t.gross_amount END) AS current_balance,
    SUM(CASE WHEN t.transaction_type = 'BUY' THEN t.total_spent_usd ELSE 0 END) AS total_invested_usd,
    CASE
        WHEN SUM(CASE WHEN t.transaction_type = 'BUY' THEN t.net_amount ELSE 0 END) > 0
        THEN SUM(CASE WHEN t.transaction_type = 'BUY' THEN t.total_spent_usd ELSE 0 END) /
             SUM(CASE WHEN t.transaction_type = 'BUY' THEN t.net_amount ELSE 0 END)
        ELSE 0
    END AS avg_buy_price
FROM transactions t
JOIN assets a ON t.asset_id = a.id
JOIN exchanges e ON t.exchange_id = e.id
GROUP BY t.user_id, a.symbol, e.name;

DROP VIEW IF EXISTS sell_opportunities;
CREATE VIEW sell_opportunities AS
SELECT
    t.user_id,
    t.id AS transaction_id,
    a.symbol,
    a.name AS asset_name,
    t.transaction_type,
    t.unit_price_usd AS buy_price,
    t.net_amount AS coin_amount,
    ss.threshold_percent,
    t.unit_price_usd * (1 + ss.threshold_percent / 100) AS target_sell_price
FROM transactions t
JOIN assets a ON t.asset_id = a.id
JOIN sell_strategies ss
    ON ss.asset_id = t.asset_id
   AND ss.user_id = t.user_id
   AND ss.is_active = true
WHERE t.transaction_type = 'BUY'
  AND ss.threshold_percent IS NOT NULL;

DROP VIEW IF EXISTS buy_opportunities;
CREATE VIEW buy_opportunities AS
SELECT
    bs.user_id,
    a.id AS asset_id,
    a.symbol,
    a.name AS asset_name,
    bs.dip_threshold_percent,
    bs.buy_amount_usd,
    pp.peak_price,
    pp.peak_price * (1 - bs.dip_threshold_percent / 100) AS target_buy_price,
    pp.last_buy_transaction_id,
    pp.peak_timestamp AS last_peak_timestamp
FROM buy_strategies bs
JOIN assets a ON bs.asset_id = a.id
LEFT JOIN price_peaks pp
    ON pp.asset_id = bs.asset_id
   AND pp.user_id = bs.user_id
   AND pp.is_active = true
WHERE bs.is_active = true;
