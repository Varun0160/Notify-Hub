package com.notifyhub.security;

import com.notifyhub.common.enums.ApiKeyStatus;
import com.notifyhub.common.enums.TenantStatus;
import com.notifyhub.tenant.ApiKey;
import com.notifyhub.tenant.ApiKeyRepository;
import com.notifyhub.tenant.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApiKeyAuthenticator {

    private static final Duration LAST_USED_REFRESH = Duration.ofMinutes(5);

    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public Optional<TenantPrincipal> authenticate(String rawKey) {
        String hash = ApiKeyGenerator.hash(rawKey);
        return apiKeyRepository.findWithTenantByKeyHash(hash)
                .filter(this::isUsable)
                .map(key -> {
                    touch(key);
                    Tenant tenant = key.getTenant();
                    return new TenantPrincipal(tenant.getId(), tenant.getPublicId(), tenant.getName());
                });
    }

    private boolean isUsable(ApiKey key) {
        Instant now = Instant.now();
        return key.getStatus() == ApiKeyStatus.ACTIVE
                && (key.getExpiresAt() == null || key.getExpiresAt().isAfter(now))
                && key.getTenant().getStatus() == TenantStatus.ACTIVE;
    }

    private void touch(ApiKey key) {
        Instant now = Instant.now();
        if (key.getLastUsedAt() == null || key.getLastUsedAt().isBefore(now.minus(LAST_USED_REFRESH))) {
            key.setLastUsedAt(now);
        }
    }
}