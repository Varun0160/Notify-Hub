ALTER TABLE notifications
    ADD COLUMN request_hash VARCHAR(64) NULL AFTER idempotency_key;