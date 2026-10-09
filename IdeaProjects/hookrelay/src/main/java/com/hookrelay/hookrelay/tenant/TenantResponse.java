package com.hookrelay.hookrelay.tenant;

import java.time.Instant;

public record TenantResponse(Long id, String name, String email, Instant createdAt) {

    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.getId(), t.getName(), t.getEmail(), t.getCreatedAt());
    }
}