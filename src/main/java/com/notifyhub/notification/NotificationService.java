package com.notifyhub.notification;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.common.exception.IdempotencyConflictException;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.recipient.QuietHours;
import com.notifyhub.recipient.Recipient;
import com.notifyhub.recipient.RecipientPreference;
import com.notifyhub.recipient.RecipientPreferenceRepository;
import com.notifyhub.recipient.RecipientRepository;
import com.notifyhub.template.Template;
import com.notifyhub.template.TemplateRenderer;
import com.notifyhub.template.TemplateRepository;
import com.notifyhub.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String REASON_OPTED_OUT = "RECIPIENT_OPTED_OUT";

    private static final Duration MAX_SCHEDULE_AHEAD = Duration.ofDays(30);
    private static final int MAX_VARIABLE_LENGTH = 1000;
    private static final int MAX_PAYLOAD_CHARS = 15_000;

    private final NotificationRepository notificationRepository;
    private final RecipientRepository recipientRepository;
    private final RecipientPreferenceRepository preferenceRepository;
    private final TemplateRepository templateRepository;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SendResult accept(Long tenantId, SendNotificationRequest request, String idempotencyKey) {
        Instant now = Instant.now();
        Channel channel = request.channel();

        String key = IdempotencyKeys.normalize(idempotencyKey);
        String fingerprint = RequestFingerprint.of(request);

        // 0. Have we already accepted this exact request? Then return the original.
        if (key != null) {
            Optional<SendResult> replay = findReplay(tenantId, key, fingerprint);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        // 1. Recipient, scoped to the caller's tenant (404 for someone else's)
        Recipient recipient = recipientRepository
                .findByPublicIdAndTenantId(request.recipientId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recipient not found: " + request.recipientId()));

        // 2. Can we even reach them on this channel?
        requireContact(recipient, channel);

        // 3. Active template for this tenant + name + channel
        Template template = templateRepository
                .findByTenantIdAndNameAndChannelAndActiveTrue(tenantId, request.template().trim(), channel)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template not found: " + request.template() + " (" + channel + ")"));

        // 4. Fail fast: dry-run render so missing variables are rejected now, not in a worker
        Map<String, String> variables = request.variables() == null ? Map.of() : request.variables();
        checkVariableLimits(variables);
        TemplateRenderer.render(channel, template.getSubject(), template.getBody(), variables);

        String payload = objectMapper.writeValueAsString(variables);
        if (payload.length() > MAX_PAYLOAD_CHARS) {
            throw new InvalidRequestException("Variables are too large");
        }

        // 5. When may it go out?
        Instant scheduledAt = resolveSchedule(request, recipient, now);

        // 6. Build the notification, pinned to this exact template row
        Notification notification = new Notification(
                tenantRepository.getReferenceById(tenantId), recipient, template, channel);
        notification.setPayload(payload);
        notification.setScheduledAt(scheduledAt);
        if (key != null) {
            notification.setIdempotencyKey(key);
            notification.setRequestHash(fingerprint);
        }

        // 7. The recipient's explicit opt-out wins over everything, including urgent
        if (!isChannelEnabled(recipient, channel)) {
            notification.transitionTo(NotificationStatus.SKIPPED);
            notification.setFailureReason(REASON_OPTED_OUT);
        }

        // The INSERT happens here. A concurrent duplicate key fails here with
        // DataIntegrityViolationException, which IdempotentSendService handles.
        Notification saved = notificationRepository.save(notification);

        // Only notifications that are QUEUED and due now go to the broker immediately.
        // SKIPPED ones, and ones deferred to a later time, do not.
        boolean dueNow = scheduledAt == null || !scheduledAt.isAfter(now);
        if (saved.getStatus() == NotificationStatus.QUEUED && dueNow) {
            eventPublisher.publishEvent(new NotificationQueuedEvent(
                    saved.getId(), saved.getPublicId(), saved.getChannel()));
        }
        return new SendResult(NotificationResponse.from(saved), false);
    }

    /** Used by IdempotentSendService after losing a race. Runs in its own, fresh transaction. */
    @Transactional(readOnly = true)
    public Optional<SendResult> replay(Long tenantId, String idempotencyKey, SendNotificationRequest request) {
        return findReplay(tenantId, idempotencyKey, RequestFingerprint.of(request));
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(Long tenantId, String notificationId) {
        Notification notification = notificationRepository
                .findByPublicIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + notificationId));
        return NotificationResponse.from(notification);
    }

    private Optional<SendResult> findReplay(Long tenantId, String key, String fingerprint) {
        return notificationRepository.findByTenantIdAndIdempotencyKey(tenantId, key)
                .map(existing -> {
                    if (existing.getRequestHash() != null && !existing.getRequestHash().equals(fingerprint)) {
                        throw new IdempotencyConflictException(
                                "Idempotency-Key was already used with a different request");
                    }
                    return new SendResult(NotificationResponse.from(existing), true);
                });
    }

    private Instant resolveSchedule(SendNotificationRequest request, Recipient recipient, Instant now) {
        Instant requested = request.scheduledAt();
        if (requested != null && requested.isAfter(now.plus(MAX_SCHEDULE_AHEAD))) {
            throw new InvalidRequestException("scheduledAt can be at most 30 days ahead");
        }

        boolean urgent = Boolean.TRUE.equals(request.urgent());
        if (urgent) {
            return requested;
        }

        Instant effective = (requested != null) ? requested : now;
        boolean quiet = QuietHours.isQuiet(recipient.getQuietHoursStart(),
                recipient.getQuietHoursEnd(), recipient.getTimezone(), effective);
        return quiet
                ? QuietHours.nextEnd(recipient.getQuietHoursEnd(), recipient.getTimezone(), effective)
                : requested;
    }

    private boolean isChannelEnabled(Recipient recipient, Channel channel) {
        return preferenceRepository.findByRecipientIdAndChannel(recipient.getId(), channel)
                .map(RecipientPreference::isEnabled)
                .orElse(true);
    }

    private void requireContact(Recipient recipient, Channel channel) {
        boolean reachable = switch (channel) {
            case EMAIL -> hasText(recipient.getEmail());
            case SMS -> hasText(recipient.getPhone());
            case PUSH -> hasText(recipient.getDeviceToken());
        };
        if (!reachable) {
            throw new InvalidRequestException("Recipient has no contact details for channel " + channel);
        }
    }

    private void checkVariableLimits(Map<String, String> variables) {
        for (String value : variables.values()) {
            if (value != null && value.length() > MAX_VARIABLE_LENGTH) {
                throw new InvalidRequestException(
                        "Variable values must be at most " + MAX_VARIABLE_LENGTH + " characters");
            }
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}