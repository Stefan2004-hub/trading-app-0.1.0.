CREATE TABLE market_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    alert_type VARCHAR(4) NOT NULL CHECK (alert_type IN ('BUY', 'SELL')),
    strategy_type VARCHAR(10) NOT NULL CHECK (strategy_type IN ('FIXED_14D', 'DYNAMIC')),
    rsi_value NUMERIC(10, 4) NOT NULL,
    stoch_value NUMERIC(10, 4) NOT NULL,
    interval_days INTEGER NOT NULL CHECK (interval_days > 0),
    trigger_date DATE NOT NULL,
    is_viewed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_market_alert_unique_trigger UNIQUE (user_id, asset_id, alert_type, strategy_type, trigger_date)
);

CREATE INDEX idx_market_alerts_user_trigger ON market_alerts(user_id, trigger_date DESC, created_at DESC);
CREATE INDEX idx_market_alerts_user_asset_trigger ON market_alerts(user_id, asset_id, trigger_date DESC);
CREATE INDEX idx_market_alerts_user_viewed_trigger ON market_alerts(user_id, is_viewed, trigger_date DESC);
