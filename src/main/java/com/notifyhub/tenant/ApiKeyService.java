package com.notifyhub.tenant;

import com.notifyhub.common.enums.ApiKeyStatus;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.security.ApiKeyGenerator;
import com.notifyhub.security.ApiKeyGenerator.GeneratedKey;
import com.notifyhub.tenant.dto.ApiKeyCreatedResponse;
import com.notifyhub.tenant.dto.ApiKeyResponse;
import com.notifyhub.tenant.dto.CreateApiKeyRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public ApiKeyCreatedResponse create(String tenantPublicId, CreateApiKeyRequest request) {
        Tenant tenant = findTenant(tenantPublicId);
        GeneratedKey generated = ApiKeyGenerator.generate();

        ApiKey key = new ApiKey(tenant, request.name(), generated.prefix(), generated.hash());
        key.setExpiresAt(request.expiresAt());
        ApiKey saved = apiKeyRepository.save(key);

        return ApiKeyCreatedResponse.from(saved, generated.rawKey());
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(String tenantPublicId) {
        Tenant tenant = findTenant(tenantPublicId);
        return apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenant.getId())
                .stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    @Transactional
    public void revoke(String tenantPublicId, String keyPublicId) {
        Tenant tenant = findTenant(tenantPublicId);
        ApiKey key = apiKeyRepository.findByPublicIdAndTenantId(keyPublicId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + keyPublicId));
        key.setStatus(ApiKeyStatus.REVOKED);   // dirty checking saves it at commit
    }

    private Tenant findTenant(String tenantPublicId) {
        return tenantRepository.findByPublicId(tenantPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantPublicId));
    }
}