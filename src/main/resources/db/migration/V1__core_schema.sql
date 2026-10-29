-- Users who can log in to the API and dashboard.
CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'ANALYST', 'SERVICE')),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One row per assessed transaction. There is deliberately no card number column:
-- callers send an opaque account id, so a database leak exposes no card data.
CREATE TABLE fraud_transaction (
    id              UUID          PRIMARY KEY,
    external_id     VARCHAR(64)   NOT NULL,
    idempotency_key VARCHAR(100)  NOT NULL UNIQUE,
    account_id      VARCHAR(64)   NOT NULL,
    merchant_id     VARCHAR(64)   NOT NULL,
    amount          NUMERIC(19,2) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)    NOT NULL,
    country         VARCHAR(2)    NOT NULL,
    device_id       VARCHAR(64),
    occurred_at     TIMESTAMPTZ   NOT NULL,
    received_at     TIMESTAMPTZ   NOT NULL,
    decision        VARCHAR(8)    NOT NULL CHECK (decision IN ('APPROVE', 'REVIEW', 'DECLINE')),
    risk_score      INTEGER       NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    request_hash    VARCHAR(64)   NOT NULL
);
-- Serves the velocity, country and card-testing lookups: "this account, recently".
CREATE INDEX idx_tx_account_time ON fraud_transaction (account_id, occurred_at DESC);
CREATE INDEX idx_tx_received ON fraud_transaction (received_at DESC);

-- Why a transaction scored what it did: one row per rule that fired.
CREATE TABLE rule_hit (
    id             BIGSERIAL    PRIMARY KEY,
    transaction_id UUID         NOT NULL REFERENCES fraud_transaction (id),
    rule_id        VARCHAR(48)  NOT NULL,
    score          INTEGER      NOT NULL,
    reason         VARCHAR(300) NOT NULL
);
CREATE INDEX idx_rule_hit_tx ON rule_hit (transaction_id);

-- Work queue for analysts. Created for every REVIEW or DECLINE.
CREATE TABLE alert (
    id             BIGSERIAL    PRIMARY KEY,
    transaction_id UUID         NOT NULL UNIQUE REFERENCES fraud_transaction (id),
    status         VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE')),
    created_at     TIMESTAMPTZ  NOT NULL,
    resolved_by    VARCHAR(64),
    resolved_at    TIMESTAMPTZ,
    note           VARCHAR(500),
    version        BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_alert_status_created ON alert (status, created_at DESC);

CREATE TABLE blacklist_entry (
    id          BIGSERIAL    PRIMARY KEY,
    kind        VARCHAR(12)  NOT NULL CHECK (kind IN ('ACCOUNT', 'MERCHANT', 'DEVICE')),
    entry_value VARCHAR(64)  NOT NULL,
    reason      VARCHAR(200),
    created_by  VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    UNIQUE (kind, entry_value)
);

-- Append-only record of who did what. The application never updates or deletes rows here.
CREATE TABLE audit_log (
    id          BIGSERIAL    PRIMARY KEY,
    occurred_at TIMESTAMPTZ  NOT NULL,
    actor       VARCHAR(64)  NOT NULL,
    action      VARCHAR(48)  NOT NULL,
    subject     VARCHAR(100) NOT NULL,
    detail      VARCHAR(500)
);
