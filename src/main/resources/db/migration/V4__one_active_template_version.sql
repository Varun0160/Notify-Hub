ALTER TABLE templates
    ADD COLUMN active_key TINYINT GENERATED ALWAYS AS (IF(active, 1, NULL)) STORED,
    ADD CONSTRAINT uk_templates_one_active UNIQUE (tenant_id, name, channel, active_key);