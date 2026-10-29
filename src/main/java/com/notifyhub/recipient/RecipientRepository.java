package com.notifyhub.recipient;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    Optional<Recipient> findByPublicId(String publicId);
    Optional<Recipient> findByTenantIdAndExternalId(Long tenantId, String externalId);
}