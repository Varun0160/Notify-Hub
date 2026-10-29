package com.notifyhub.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByPublicId(String publicId);
    Optional<Notification> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);
}