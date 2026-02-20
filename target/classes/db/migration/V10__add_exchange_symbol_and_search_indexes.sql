-- Add exchange symbol for CRUD/search parity with assets.

ALTER TABLE exchanges
    ADD COLUMN symbol VARCHAR(10);

UPDATE exchanges
SET symbol = UPPER(LEFT(REGEXP_REPLACE(name, '[^A-Za-z0-9]', '', 'g'), 10))
WHERE symbol IS NULL OR symbol = '';

ALTER TABLE exchanges
    ALTER COLUMN symbol SET NOT NULL;

ALTER TABLE exchanges
    ADD CONSTRAINT uk_exchanges_symbol UNIQUE (symbol);

CREATE INDEX idx_assets_symbol_name_search ON assets (LOWER(symbol), LOWER(name));
CREATE INDEX idx_exchanges_symbol_name_search ON exchanges (LOWER(symbol), LOWER(name));
