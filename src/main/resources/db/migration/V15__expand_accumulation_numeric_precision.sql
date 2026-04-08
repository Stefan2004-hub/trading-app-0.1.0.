-- 1. Drop the generated column first
ALTER TABLE accumulation_trades 
    DROP COLUMN accumulation_delta;

-- 2. Alter the base numeric columns to the higher precision
ALTER TABLE accumulation_trades
    ALTER COLUMN old_coin_amount TYPE NUMERIC(38, 18),
    ALTER COLUMN new_coin_amount TYPE NUMERIC(38, 18),
    ALTER COLUMN exit_price_usd TYPE NUMERIC(38, 18),
    ALTER COLUMN reentry_price_usd TYPE NUMERIC(38, 18);

-- 3. Re-add the generated column with the updated precision
ALTER TABLE accumulation_trades
    ADD COLUMN accumulation_delta NUMERIC(38, 18) GENERATED ALWAYS AS (
        COALESCE(new_coin_amount, 0) - old_coin_amount
    ) STORED;