package com.hookrelay.hookrelay.tenant;

import com.hookrelay.hookrelay.common.DuplicateResourceException;
import com.hookrelay.hookrelay.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest request) {
        String email = request.email().trim().toLowerCase();
        if (tenantRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A tenant with this email already exists");
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.name().trim());
        tenant.setEmail(email);
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public TenantResponse getById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
        return TenantResponse.from(tenant);
    }
}