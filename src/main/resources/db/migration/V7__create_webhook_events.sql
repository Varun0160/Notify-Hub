CREATE TABLE webhook_events (
                                id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                source               VARCHAR(30)  NOT NULL,
                                event_id             VARCHAR(100) NOT NULL,
                                provider_message_id  VARCHAR(255) NOT NULL,
                                event_type           VARCHAR(30)  NOT NULL,
                                outcome              VARCHAR(20)  NOT NULL,
                                created_at           DATETIME(6)  NOT NULL,
                                updated_at           DATETIME(6)  NOT NULL,
                                CONSTRAINT uk_webhook_source_event UNIQUE (source, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;