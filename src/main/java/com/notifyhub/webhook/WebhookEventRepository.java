package com.notifyhub.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsBySourceAndEventId(String source, String eventId);
}