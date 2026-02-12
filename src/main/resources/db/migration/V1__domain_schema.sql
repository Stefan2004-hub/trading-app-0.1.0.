-- V1 domain schema
-- Section for point 5: base market/reference tables.

CREATE TABLE btc_historic_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_date DATE NOT NULL UNIQUE,
    high_price NUMERIC(20, 8) NOT NULL,
    low_price NUMERIC(20, 8) NOT NULL,
    closing_price NUMERIC(20, 8) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE exchanges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID REFERENCES assets(id),
    exchange_id UUID REFERENCES exchanges(id),
    transaction_type VARCHAR(4) CHECK (transaction_type IN ('BUY', 'SELL')),
    gross_amount NUMERIC(20, 18) NOT NULL,
    fee_amount NUMERIC(20, 18) DEFAULT 0,
    fee_currency VARCHAR(10),
    net_amount NUMERIC(20, 18) NOT NULL,
    unit_price_usd NUMERIC(20, 18) NOT NULL,
    total_spent_usd NUMERIC(20, 18) NOT NULL,
    transaction_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE VIEW user_portfolio_performance AS
SELECT
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
GROUP BY a.symbol, e.name;

ALTER TABLE transactions ADD COLUMN realized_pnl NUMERIC(20, 18);

CREATE TABLE accumulation_trades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exit_transaction_id UUID NOT NULL REFERENCES transactions(id),
    reentry_transaction_id UUID REFERENCES transactions(id),
    asset_id UUID NOT NULL REFERENCES assets(id),
    old_coin_amount NUMERIC(20, 18) NOT NULL,
    new_coin_amount NUMERIC(20, 18),
    accumulation_delta NUMERIC(20, 18) GENERATED ALWAYS AS (
        COALESCE(new_coin_amount, 0) - old_coin_amount
    ) STORED,
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    exit_price_usd NUMERIC(20, 18) NOT NULL,
    reentry_price_usd NUMERIC(20, 18),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    prediction_notes TEXT
);

CREATE INDEX idx_accumulation_trades_status ON accumulation_trades(status);
CREATE INDEX idx_accumulation_trades_asset ON accumulation_trades(asset_id);

CREATE TABLE sell_strategies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id),
    threshold_percent NUMERIC(5, 2) NOT NULL CHECK (threshold_percent > 0),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(asset_id)
);

CREATE INDEX idx_sell_strategies_active ON sell_strategies(asset_id, is_active);

CREATE TABLE buy_strategies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id),
    dip_threshold_percent NUMERIC(5, 2) NOT NULL CHECK (dip_threshold_percent > 0),
    buy_amount_usd NUMERIC(20, 2) NOT NULL CHECK (buy_amount_usd > 0),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(asset_id)
);

CREATE INDEX idx_buy_strategies_active ON buy_strategies(asset_id, is_active);

CREATE TABLE strategy_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL REFERENCES assets(id),
    strategy_type VARCHAR(4) NOT NULL CHECK (strategy_type IN ('BUY', 'SELL')),
    trigger_price NUMERIC(20, 8) NOT NULL,
    threshold_percent NUMERIC(5, 2) NOT NULL,
    reference_price NUMERIC(20, 8) NOT NULL,
    alert_message TEXT,
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACKNOWLEDGED', 'EXECUTED', 'DISMISSED')),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_strategy_alerts_asset ON strategy_alerts(asset_id);
CREATE INDEX idx_strategy_alerts_status ON strategy_alerts(status);
CREATE INDEX idx_strategy_alerts_pending ON strategy_alerts(asset_id, strategy_type, status) WHERE status = 'PENDING';

CREATE TABLE price_peaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id UUID NOT NULL UNIQUE REFERENCES assets(id),
    last_buy_transaction_id UUID REFERENCES transactions(id),
    peak_price NUMERIC(20, 8) NOT NULL,
    peak_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_price_peaks_asset_active ON price_peaks(asset_id, is_active);
