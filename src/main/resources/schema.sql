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

-- ตารางผู้ใช้สำหรับ JWT Login
CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_app_user_username ON app_user(username);
