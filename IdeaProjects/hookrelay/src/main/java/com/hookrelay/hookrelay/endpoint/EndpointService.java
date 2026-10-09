package com.hookrelay.hookrelay.endpoint;

import com.hookrelay.hookrelay.common.ResourceNotFoundException;
import com.hookrelay.hookrelay.tenant.Tenant;
import com.hookrelay.hookrelay.tenant.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;

@Service
public class EndpointService {

    private final EndpointRepository endpointRepository;
    private final TenantRepository tenantRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EndpointService(EndpointRepository endpointRepository, TenantRepository tenantRepository) {
        this.endpointRepository = endpointRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public EndpointResponse create(Long tenantId, CreateEndpointRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        Endpoint endpoint = new Endpoint();
        endpoint.setTenant(tenant);
        endpoint.setUrl(request.url().trim());
        endpoint.setSecret(generateSecret());
        endpoint.setEventTypes(request.eventTypes());
        return EndpointResponse.from(endpointRepository.save(endpoint));
    }

    @Transactional(readOnly = true)
    public Page<EndpointResponse> list(Long tenantId, Pageable pageable) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found: " + tenantId);
        }
        return endpointRepository.findByTenantId(tenantId, pageable).map(EndpointResponse::from);
    }

    @Transactional(readOnly = true)
    public EndpointResponse get(Long tenantId, Long endpointId) {
        return EndpointResponse.from(find(tenantId, endpointId));
    }

    @Transactional
    public void delete(Long tenantId, Long endpointId) {
        endpointRepository.delete(find(tenantId, endpointId));
    }

    private Endpoint find(Long tenantId, Long endpointId) {
        return endpointRepository.findByIdAndTenantId(endpointId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found: " + endpointId));
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "whsec_" + HexFormat.of().formatHex(bytes);
    }
}