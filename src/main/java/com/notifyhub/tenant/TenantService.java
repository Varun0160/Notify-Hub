package com.notifyhub.tenant;

import com.notifyhub.common.exception.DuplicateResourceException;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.tenant.dto.CreateTenantRequest;
import com.notifyhub.tenant.dto.TenantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        if (tenantRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tenant already exists: " + request.name());
        }
        Tenant saved = tenantRepository.save(new Tenant(request.name()));
        return TenantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TenantResponse getByPublicId(String publicId) {
        Tenant tenant = tenantRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + publicId));
        return TenantResponse.from(tenant);
    }
}