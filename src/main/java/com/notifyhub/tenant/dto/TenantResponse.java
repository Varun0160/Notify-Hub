package com.notifyhub.tenant.dto;

import com.notifyhub.tenant.Tenant;

import java.time.Instant;

public record TenantResponse(
        String id,
        String name,
        String status,
        Instant createdAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getPublicId(),
                tenant.getName(),
                tenant.getStatus().name(),
                tenant.getCreatedAt()
        );
    }
}