package com.notifyhub.security;

public record TenantPrincipal(Long tenantId, String tenantPublicId, String tenantName) {
}