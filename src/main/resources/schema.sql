-- ตารางเก็บ snapshot ราคา Crypto (ประวัติสำหรับกราฟ/วิเคราะห์)
CREATE TABLE IF NOT EXISTS price_snapshot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    price VARCHAR(50) NOT NULL,
    change_24h VARCHAR(20),
    volume_24h VARCHAR(50),
    recorded_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_price_snapshot_symbol ON price_snapshot(symbol);
CREATE INDEX IF NOT EXISTS idx_price_snapshot_recorded_at ON price_snapshot(recorded_at);
