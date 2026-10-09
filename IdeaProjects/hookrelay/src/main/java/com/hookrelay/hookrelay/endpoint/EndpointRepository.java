package com.hookrelay.hookrelay.endpoint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    Page<Endpoint> findByTenantId(Long tenantId, Pageable pageable);

    Optional<Endpoint> findByIdAndTenantId(Long id, Long tenantId);
}