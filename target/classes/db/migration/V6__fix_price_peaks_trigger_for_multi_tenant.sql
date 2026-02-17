ALTER TABLE price_peaks
    DROP CONSTRAINT IF EXISTS price_peaks_asset_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_price_peaks_user_asset
    ON price_peaks(user_id, asset_id);

CREATE OR REPLACE FUNCTION reset_price_peak_on_buy()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO price_peaks (user_id, asset_id, last_buy_transaction_id, peak_price, peak_timestamp, is_active)
    VALUES (NEW.user_id, NEW.asset_id, NEW.id, NEW.unit_price_usd, NEW.transaction_date, true)
    ON CONFLICT (user_id, asset_id)
    DO UPDATE SET
        last_buy_transaction_id = NEW.id,
        peak_price = NEW.unit_price_usd,
        peak_timestamp = NEW.transaction_date,
        is_active = true,
        updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
