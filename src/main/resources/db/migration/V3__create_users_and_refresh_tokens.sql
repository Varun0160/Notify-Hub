CREATE TABLE users (
                       id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                       public_id      VARCHAR(36)  NOT NULL,
                       email          VARCHAR(255) NOT NULL,
                       password_hash  VARCHAR(100) NOT NULL,
                       full_name      VARCHAR(100) NOT NULL,
                       role           VARCHAR(20)  NOT NULL,
                       enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at     DATETIME(6)  NOT NULL,
                       updated_at     DATETIME(6)  NOT NULL,
                       CONSTRAINT uk_users_public_id UNIQUE (public_id),
                       CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refresh_tokens (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id     BIGINT      NOT NULL,
                                token_hash  VARCHAR(64) NOT NULL,
                                expires_at  DATETIME(6) NOT NULL,
                                revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
                                created_at  DATETIME(6) NOT NULL,
                                updated_at  DATETIME(6) NOT NULL,
                                CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
                                CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id),
                                INDEX idx_refresh_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;