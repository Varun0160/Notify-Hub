ALTER TABLE notifications
    ADD COLUMN published_at DATETIME(6) NULL AFTER sent_at;