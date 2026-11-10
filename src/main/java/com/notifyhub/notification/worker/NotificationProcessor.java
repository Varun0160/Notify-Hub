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

    public static final String STALE_REASON = "Worker did not report a result (crash or timeout)";

    private final NotificationRepository notificationRepository;
    private final NotificationAttemptRepository attemptRepository;
    private final ObjectMapper objectMapper;
    private final RetryPolicy retryPolicy;

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
                retryOrFail(notification, attemptNo, outcome.error());
            }
        }
        attemptRepository.save(attempt);
    }

    /**
     * Called by the reaper for a notification whose worker never reported back.
     * Returns false if the row is no longer PROCESSING (the worker finished after all).
     */
    @Transactional
    public boolean recoverStale(Long id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null || notification.getStatus() != NotificationStatus.PROCESSING) {
            return false;
        }
        int attemptNo = notification.getRetryCount() + 1;

        NotificationAttempt attempt = new NotificationAttempt(notification, attemptNo, "reaper");
        attempt.setStatus(AttemptStatus.FAILED);
        attempt.setErrorMessage(STALE_REASON);
        attemptRepository.save(attempt);

        retryOrFail(notification, attemptNo, STALE_REASON);
        return true;
    }

    // One rule for every retryable problem, whether it came from a provider or from a dead worker.
    private void retryOrFail(Notification notification, int attemptNo, String error) {
        if (attemptNo >= retryPolicy.maxAttempts()) {
            notification.fail("RETRIES_EXHAUSTED: " + error);
        } else {
            notification.requeueForRetry(error, retryPolicy.nextAttemptAt(Instant.now(), attemptNo));
        }
    }

    private static String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : s.substring(0, max);
    }
}