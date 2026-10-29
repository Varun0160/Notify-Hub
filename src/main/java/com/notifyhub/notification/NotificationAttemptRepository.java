package com.notifyhub.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {
    List<NotificationAttempt> findByNotificationIdOrderByAttemptNoAsc(Long notificationId);
    Optional<NotificationAttempt> findByProviderMessageId(String providerMessageId);
}