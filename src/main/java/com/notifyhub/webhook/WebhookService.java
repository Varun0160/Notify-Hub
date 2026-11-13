package com.notifyhub.webhook;

import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationAttempt;
import com.notifyhub.notification.NotificationAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    public static final String SOURCE = "provider";
    public static final String REASON_BOUNCED = "BOUNCED";

    private final NotificationAttemptRepository attemptRepository;
    private final WebhookEventRepository eventRepository;

    @Transactional
    public WebhookResult handle(WebhookPayload payload) {
        // Layer 2: have we already processed this event id?
        if (eventRepository.existsBySourceAndEventId(SOURCE, payload.eventId())) {
            return WebhookResult.DUPLICATE;
        }

        // Unknown message id: answer 404 so the provider retries (our complete() may not have committed yet).
        // Nothing is stored, so the retry is processed normally.
        NotificationAttempt attempt = attemptRepository.findByProviderMessageId(payload.providerMessageId())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown message"));
        Notification notification = attempt.getNotification();

        WebhookResult result = apply(notification, payload.type());

        eventRepository.save(new WebhookEvent(
                SOURCE, payload.eventId(), payload.providerMessageId(), payload.type(), result));
        return result;
    }

    // Layer 3: the state machine decides what is allowed.
    private WebhookResult apply(Notification notification, String type) {
        return switch (type.toLowerCase()) {
            case "delivered" -> moveIfPossible(notification, NotificationStatus.DELIVERED, null);
            case "bounced" -> moveIfPossible(notification, NotificationStatus.FAILED, REASON_BOUNCED);
            default -> WebhookResult.IGNORED;   // "opened", and any event type we don't know yet
        };
    }

    private WebhookResult moveIfPossible(Notification notification, NotificationStatus target, String failureReason) {
        if (!notification.getStatus().canTransitionTo(target)) {
            log.info("Ignoring webhook: {} cannot move to {}", notification.getStatus(), target);
            return WebhookResult.IGNORED;
        }
        if (target == NotificationStatus.FAILED) {
            notification.fail(failureReason);
        } else {
            notification.transitionTo(target);
        }
        return WebhookResult.APPLIED;
    }
}