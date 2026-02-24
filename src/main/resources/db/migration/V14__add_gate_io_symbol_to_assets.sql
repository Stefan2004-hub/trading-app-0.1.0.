ALTER TABLE assets
    ADD COLUMN gate_io_symbol VARCHAR(20);

CREATE INDEX idx_assets_gate_io_symbol ON assets(gate_io_symbol);
