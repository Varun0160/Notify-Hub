CREATE TABLE users (
                       id            BIGINT       NOT NULL AUTO_INCREMENT,
                       tenant_id     BIGINT       NOT NULL,
                       email         VARCHAR(150) NOT NULL,
                       password_hash VARCHAR(100) NOT NULL,
                       role          VARCHAR(20)  NOT NULL,
                       created_at    TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       PRIMARY KEY (id),
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
                       INDEX idx_users_tenant (tenant_id)
);