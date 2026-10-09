package com.hookrelay.hookrelay.endpoint;

import java.time.Instant;
import java.util.Set;

public record EndpointResponse(Long id, Long tenantId, String url, boolean active,
                               Set<String> eventTypes, Instant createdAt) {

    public static EndpointResponse from(Endpoint e) {
        return new EndpointResponse(e.getId(), e.getTenant().getId(), e.getUrl(),
                e.isActive(), e.getEventTypes(), e.getCreatedAt());
    }
}