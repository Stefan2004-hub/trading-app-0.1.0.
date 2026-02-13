-- V5 seed data for lookup endpoints

INSERT INTO assets (id, symbol, name)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'BTC', 'Bitcoin'),
    ('22222222-2222-2222-2222-222222222222', 'ETH', 'Ethereum'),
    ('33333333-3333-3333-3333-333333333333', 'SOL', 'Solana'),
    ('44444444-4444-4444-4444-444444444444', 'ADA', 'Cardano'),
    ('55555555-5555-5555-5555-555555555555', 'BNB', 'BNB')
ON CONFLICT (symbol) DO NOTHING;

INSERT INTO exchanges (id, name)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Binance'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Coinbase'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Kraken'),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Bybit'),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'OKX')
ON CONFLICT (name) DO NOTHING;
