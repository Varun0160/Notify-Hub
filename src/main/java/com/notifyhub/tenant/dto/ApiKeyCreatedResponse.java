package com.notifyhub.tenant.dto;

import com.notifyhub.tenant.ApiKey;

import java.time.Instant;

public record ApiKeyCreatedResponse(
        String id,
        String name,
        String apiKey,
        String keyPrefix,
        Instant expiresAt,
        Instant createdAt
) {
    public static ApiKeyCreatedResponse from(ApiKey key, String rawKey) {
        return new ApiKeyCreatedResponse(
                key.getPublicId(), key.getName(), rawKey,
                key.getKeyPrefix(), key.getExpiresAt(), key.getCreatedAt());
    }
}