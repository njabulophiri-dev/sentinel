CREATE TABLE login_attempt (
    key_hash          VARCHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    failures          INTEGER NOT NULL CHECK (failures > 0)
);

CREATE INDEX idx_login_attempt_expiry ON login_attempt (expires_at);
