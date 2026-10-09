package com.hookrelay.hookrelay.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class CurrentTenant {

    private CurrentTenant() {}

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getClaimAsString("tenantId"));
    }
}