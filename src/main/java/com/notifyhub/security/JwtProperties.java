package com.notifyhub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifyhub.jwt")
public record JwtProperties(String secret, String issuer, long accessTtlMinutes, long refreshTtlDays) {
}