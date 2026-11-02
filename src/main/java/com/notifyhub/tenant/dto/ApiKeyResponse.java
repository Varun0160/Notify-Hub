package com.notifyhub.tenant.dto;

import com.notifyhub.tenant.ApiKey;

import java.time.Instant;

public record ApiKeyResponse(
        String id,
        String name,
        String keyPrefix,
        String status,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant createdAt
) {
    public static ApiKeyResponse from(ApiKey key) {
        return new ApiKeyResponse(
                key.getPublicId(), key.getName(), key.getKeyPrefix(),
                key.getStatus().name(), key.getExpiresAt(),
                key.getLastUsedAt(), key.getCreatedAt());
    }
}