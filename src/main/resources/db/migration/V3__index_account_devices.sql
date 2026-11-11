CREATE INDEX idx_tx_account_device
    ON fraud_transaction (account_id, device_id)
    WHERE device_id IS NOT NULL;
