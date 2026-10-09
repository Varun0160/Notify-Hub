CREATE TABLE endpoints (
                           id         BIGINT       NOT NULL AUTO_INCREMENT,
                           tenant_id  BIGINT       NOT NULL,
                           url        VARCHAR(500) NOT NULL,
                           secret     VARCHAR(100) NOT NULL,
                           active     BOOLEAN      NOT NULL DEFAULT TRUE,
                           created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           PRIMARY KEY (id),
                           CONSTRAINT fk_endpoints_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
                           INDEX idx_endpoints_tenant (tenant_id)
);

CREATE TABLE endpoint_event_types (
                                      endpoint_id BIGINT       NOT NULL,
                                      event_type  VARCHAR(100) NOT NULL,
                                      PRIMARY KEY (endpoint_id, event_type),
                                      CONSTRAINT fk_eet_endpoint FOREIGN KEY (endpoint_id) REFERENCES endpoints (id) ON DELETE CASCADE
);