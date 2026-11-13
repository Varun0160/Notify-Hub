ALTER TABLE notifications
    ADD COLUMN lease_until DATETIME(6) NULL AFTER published_at;

CREATE INDEX idx_notifications_tenant_id ON notifications (tenant_id, id);