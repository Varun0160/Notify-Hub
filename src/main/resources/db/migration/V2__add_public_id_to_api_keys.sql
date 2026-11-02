ALTER TABLE api_keys
    ADD COLUMN public_id VARCHAR(36) NOT NULL AFTER id,ADD CONSTRAINT uk_api_keys_public_id UNIQUE (public_id);