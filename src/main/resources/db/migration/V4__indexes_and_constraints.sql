CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_oauth_accounts_provider_user ON oauth_accounts(provider, provider_user_id);
CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

CREATE INDEX idx_transactions_user_date ON transactions(user_id, transaction_date DESC);
CREATE INDEX idx_transactions_user_asset_type ON transactions(user_id, asset_id, transaction_type);

CREATE INDEX idx_accumulation_trades_user_status ON accumulation_trades(user_id, status);
CREATE INDEX idx_sell_strategies_user_asset_active ON sell_strategies(user_id, asset_id, is_active);
CREATE INDEX idx_buy_strategies_user_asset_active ON buy_strategies(user_id, asset_id, is_active);
CREATE INDEX idx_strategy_alerts_user_status_created ON strategy_alerts(user_id, status, created_at DESC);
CREATE INDEX idx_price_peaks_user_asset_active ON price_peaks(user_id, asset_id, is_active);
