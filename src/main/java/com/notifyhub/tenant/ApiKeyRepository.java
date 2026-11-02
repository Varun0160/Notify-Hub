package com.notifyhub.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    @Query("select k from ApiKey k join fetch k.tenant where k.keyHash = :keyHash")
    Optional<ApiKey> findWithTenantByKeyHash(@Param("keyHash") String keyHash);

    Optional<ApiKey> findByPublicIdAndTenantId(String publicId, Long tenantId);

    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}