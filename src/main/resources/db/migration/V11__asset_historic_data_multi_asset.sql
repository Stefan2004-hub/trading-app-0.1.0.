-- Rename BTC-only historical table to multi-asset table and enforce (day_date, asset_id) uniqueness.

ALTER TABLE btc_historic_data
    RENAME TO asset_historic_data;

ALTER TABLE asset_historic_data
    ADD COLUMN asset_id UUID;

UPDATE asset_historic_data
SET asset_id = '11111111-1111-1111-1111-111111111111'
WHERE asset_id IS NULL;

ALTER TABLE asset_historic_data
    ALTER COLUMN asset_id SET NOT NULL;

ALTER TABLE asset_historic_data
    ADD CONSTRAINT fk_asset_historic_data_asset
        FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE;

ALTER TABLE asset_historic_data
    DROP CONSTRAINT IF EXISTS btc_historic_data_day_date_key;

ALTER TABLE asset_historic_data
    DROP CONSTRAINT IF EXISTS asset_historic_data_day_date_key;

ALTER TABLE asset_historic_data
    ADD CONSTRAINT uk_asset_historic_data_day_asset UNIQUE (day_date, asset_id);

CREATE INDEX idx_asset_historic_data_asset_day ON asset_historic_data(asset_id, day_date DESC);
