CREATE TABLE tenants (
                         id         BIGINT       NOT NULL AUTO_INCREMENT,
                         name       VARCHAR(100) NOT NULL,
                         email      VARCHAR(150) NOT NULL,
                         created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         PRIMARY KEY (id),
                         CONSTRAINT uk_tenants_email UNIQUE (email)
);