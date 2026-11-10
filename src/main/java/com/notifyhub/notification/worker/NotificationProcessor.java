package com.notifyhub.notification.worker;

import com.notifyhub.common.enums.AttemptStatus;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationAttempt;
import com.notifyhub.notification.NotificationAttemptRepository;
import com.notifyhub.notification.NotificationRepository;
import com.notifyhub.provider.OutboundMessage;
import com.notifyhub.provider.SendOutcome;
import com.notifyhub.recipient.Recipient;
import com.notifyhub.template.Template;
import com.notifyhub.template.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationProcessor {

    public static final int MAX_ATTEMPTS = 3;

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;

    /** Transaction 1: take ownership. Throws ObjectOptimisticLockingFailureException if we lose a race. */
    @Transactional
    public ClaimResult claim(String publicId) {
        Optional<Notification> found = notificationRepository.findByPublicId(publicId);
        if (found.isEmpty()) {
            return ClaimResult.NOT_FOUND;
        }
        Notification notification = found.get();
        if (notification.getStatus() != NotificationStatus.QUEUED) {
            return ClaimResult.ALREADY_HANDLED;
        }
        notification.transitionTo(NotificationStatus.PROCESSING);
        notificationRepository.saveAndFlush(notification);
        return ClaimResult.CLAIMED;
    }

    /** Transaction 2 (read-only): load everything the provider needs and render it. */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public OutboundMessage prepare(String publicId) {
        Notification notification = notificationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalStateException("Notification vanished: " + publicId));

        Template template = notification.getTemplate();
        if (template == null) {
            throw new InvalidRequestException("Notification has no template");
        }
        Recipient recipient = notification.getRecipient();
        Channel channel = notification.getChannel();

        Map<String, String> variables = Map.of();
        if (notification.getPayload() != null && !notification.getPayload().isBlank()) {
            variables = objectMapper.readValue(notification.getPayload(), Map.class);
        }

        TemplateRenderer.RenderedMessage rendered =
                TemplateRenderer.render(channel, template.getSubject(), template.getBody(), variables);

        String to = switch (channel) {
            case EMAIL -> recipient.getEmail();
            case SMS -> recipient.getPhone();
            case PUSH -> recipient.getDeviceToken();
        };
        return new OutboundMessage(channel, to, rendered.subject(), rendered.body());
    }

    /** Transaction 3: record what happened and move the notification to its next state. */
    @Transactional
    public void complete(String publicId, SendOutcome outcome, String providerName) {
        Notification notification = notificationRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalStateException("Notification vanished: " + publicId));

        int attemptNo = notification.getRetryCount() + 1;
        NotificationAttempt attempt = new NotificationAttempt(notification, attemptNo, providerName);

        switch (outcome.status()) {
            case SUCCESS -> {
                attempt.setStatus(AttemptStatus.SUCCESS);
                attempt.setProviderMessageId(outcome.providerMessageId());
                notification.transitionTo(NotificationStatus.SENT);
                notification.setSentAt(Instant.now());
                notification.setFailureReason(null);
            }
            case PERMANENT_FAILURE -> {
                attempt.setStatus(AttemptStatus.FAILED);
                attempt.setErrorMessage(truncate(outcome.error(), 1000));
                notification.fail(outcome.error());
            }
            case RETRYABLE_FAILURE -> {
                attempt.setStatus(AttemptStatus.FAILED);
                attempt.setErrorMessage(truncate(outcome.error(), 1000));
                if (attemptNo >= MAX_ATTEMPTS) {
                    notification.fail("RETRIES_EXHAUSTED: " + outcome.error());
                } else {
                    notification.requeueForRetry(outcome.error());
                }
            }
        }
        attemptRepository.save(attempt);
    }

    private static String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }
}