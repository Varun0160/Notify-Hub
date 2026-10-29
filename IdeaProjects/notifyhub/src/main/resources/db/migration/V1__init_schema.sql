CREATE TABLE tenants (
                         id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                         public_id   VARCHAR(36)  NOT NULL,
                         name        VARCHAR(100) NOT NULL,
                         status      VARCHAR(20)  NOT NULL,
                         created_at  DATETIME(6)  NOT NULL,
                         updated_at  DATETIME(6)  NOT NULL,
                         CONSTRAINT uk_tenants_public_id UNIQUE (public_id),
                         CONSTRAINT uk_tenants_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE api_keys (
                          id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                          tenant_id     BIGINT       NOT NULL,
                          name          VARCHAR(100) NOT NULL,
                          key_prefix    VARCHAR(12)  NOT NULL,
                          key_hash      VARCHAR(64)  NOT NULL,
                          status        VARCHAR(20)  NOT NULL,
                          expires_at    DATETIME(6)  NULL,
                          last_used_at  DATETIME(6)  NULL,
                          created_at    DATETIME(6)  NOT NULL,
                          updated_at    DATETIME(6)  NOT NULL,
                          CONSTRAINT uk_api_keys_hash UNIQUE (key_hash),
                          CONSTRAINT fk_api_keys_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recipients (
                            id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                            public_id          VARCHAR(36)  NOT NULL,
                            tenant_id          BIGINT       NOT NULL,
                            external_id        VARCHAR(100) NOT NULL,
                            email              VARCHAR(255) NULL,
                            phone              VARCHAR(20)  NULL,
                            device_token       VARCHAR(255) NULL,
                            timezone           VARCHAR(50)  NOT NULL DEFAULT 'UTC',
                            quiet_hours_start  TIME NULL,
                            quiet_hours_end    TIME NULL,
                            created_at         DATETIME(6)  NOT NULL,
                            updated_at         DATETIME(6)  NOT NULL,
                            CONSTRAINT uk_recipients_public_id UNIQUE (public_id),
                            CONSTRAINT uk_recipients_tenant_external UNIQUE (tenant_id, external_id),
                            CONSTRAINT fk_recipients_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE recipient_preferences (
                                       id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       recipient_id  BIGINT      NOT NULL,
                                       channel       VARCHAR(10) NOT NULL,
                                       enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
                                       created_at    DATETIME(6) NOT NULL,
                                       updated_at    DATETIME(6) NOT NULL,
                                       CONSTRAINT uk_pref_recipient_channel UNIQUE (recipient_id, channel),
                                       CONSTRAINT fk_pref_recipient FOREIGN KEY (recipient_id) REFERENCES recipients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE templates (
                           id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                           tenant_id   BIGINT       NOT NULL,
                           name        VARCHAR(100) NOT NULL,
                           channel     VARCHAR(10)  NOT NULL,
                           version_no  INT          NOT NULL,
                           subject     VARCHAR(255) NULL,
                           body        TEXT         NOT NULL,
                           active      BOOLEAN      NOT NULL DEFAULT TRUE,
                           created_at  DATETIME(6)  NOT NULL,
                           updated_at  DATETIME(6)  NOT NULL,
                           CONSTRAINT uk_templates_name_channel_version UNIQUE (tenant_id, name, channel, version_no),
                           CONSTRAINT fk_templates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
                               id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                               public_id        VARCHAR(36)  NOT NULL,
                               tenant_id        BIGINT       NOT NULL,
                               recipient_id     BIGINT       NOT NULL,
                               template_id      BIGINT       NULL,
                               channel          VARCHAR(10)  NOT NULL,
                               status           VARCHAR(20)  NOT NULL,
                               payload          TEXT         NULL,
                               idempotency_key  VARCHAR(100) NULL,
                               scheduled_at     DATETIME(6)  NULL,
                               sent_at          DATETIME(6)  NULL,
                               retry_count      INT          NOT NULL DEFAULT 0,
                               failure_reason   VARCHAR(500) NULL,
                               version          BIGINT       NOT NULL DEFAULT 0,
                               created_at       DATETIME(6)  NOT NULL,
                               updated_at       DATETIME(6)  NOT NULL,
                               CONSTRAINT uk_notifications_public_id UNIQUE (public_id),
                               CONSTRAINT uk_notifications_idempotency UNIQUE (tenant_id, idempotency_key),
                               CONSTRAINT fk_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
                               CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES recipients (id),
                               CONSTRAINT fk_notifications_template FOREIGN KEY (template_id) REFERENCES templates (id),
                               INDEX idx_notifications_status_scheduled (status, scheduled_at),
                               INDEX idx_notifications_recipient_created (recipient_id, created_at),
                               INDEX idx_notifications_tenant_created (tenant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_attempts (
                                       id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       notification_id       BIGINT        NOT NULL,
                                       attempt_no            INT           NOT NULL,
                                       provider              VARCHAR(50)   NOT NULL,
                                       status                VARCHAR(20)   NOT NULL,
                                       provider_message_id   VARCHAR(255)  NULL,
                                       error_message         VARCHAR(1000) NULL,
                                       created_at            DATETIME(6)   NOT NULL,
                                       updated_at            DATETIME(6)   NOT NULL,
                                       CONSTRAINT uk_attempt_notification_no UNIQUE (notification_id, attempt_no),
                                       CONSTRAINT fk_attempt_notification FOREIGN KEY (notification_id) REFERENCES notifications (id),
                                       INDEX idx_attempt_provider_message (provider_message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

