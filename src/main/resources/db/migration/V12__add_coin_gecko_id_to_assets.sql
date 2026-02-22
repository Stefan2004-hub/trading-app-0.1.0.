-- Add explicit CoinGecko mapping to assets for deterministic historical sync.

ALTER TABLE assets
    ADD COLUMN coin_gecko_id VARCHAR(120);

UPDATE assets
SET coin_gecko_id = 'bitcoin'
WHERE UPPER(symbol) = 'BTC' AND coin_gecko_id IS NULL;

UPDATE assets
SET coin_gecko_id = 'ethereum'
WHERE UPPER(symbol) = 'ETH' AND coin_gecko_id IS NULL;

UPDATE assets
SET coin_gecko_id = 'solana'
WHERE UPPER(symbol) = 'SOL' AND coin_gecko_id IS NULL;

UPDATE assets
SET coin_gecko_id = 'cardano'
WHERE UPPER(symbol) = 'ADA' AND coin_gecko_id IS NULL;

UPDATE assets
SET coin_gecko_id = 'binancecoin'
WHERE UPPER(symbol) = 'BNB' AND coin_gecko_id IS NULL;

UPDATE assets
SET coin_gecko_id = '0x'
WHERE UPPER(symbol) = 'ZRX' AND coin_gecko_id IS NULL;

UPDATE assets
SET coin_gecko_id = 'polygon-ecosystem-token'
WHERE UPPER(symbol) = 'POL' AND coin_gecko_id IS NULL;

CREATE UNIQUE INDEX uk_assets_coin_gecko_id_ci
    ON assets (LOWER(coin_gecko_id))
    WHERE coin_gecko_id IS NOT NULL;
