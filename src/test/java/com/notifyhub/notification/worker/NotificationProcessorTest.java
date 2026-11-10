package com.notifyhub.notification.worker;

import com.notifyhub.common.enums.AttemptStatus;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationAttempt;
import com.notifyhub.notification.NotificationAttemptRepository;
import com.notifyhub.notification.NotificationRepository;
import com.notifyhub.provider.SendOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationProcessorTest {

    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
    private final NotificationProcessor processor =
            new NotificationProcessor(notifications, attempts, JsonMapper.builder().build());

    private Notification queuedNotification(String id) {
        Notification n = new Notification(null, null, null, Channel.EMAIL);
        when(notifications.findByPublicId(id)).thenReturn(Optional.of(n));
        return n;
    }

    @Test
    void claimMovesQueuedToProcessing() {
        Notification n = queuedNotification("n1");

        assertEquals(ClaimResult.CLAIMED, processor.claim("n1"));
        assertEquals(NotificationStatus.PROCESSING, n.getStatus());
    }

    @Test
    void secondClaimOfTheSameNotificationIsRejected() {
        queuedNotification("n1");

        processor.claim("n1");
        assertEquals(ClaimResult.ALREADY_HANDLED, processor.claim("n1"));
    }

    @Test
    void unknownNotificationIsNotFound() {
        when(notifications.findByPublicId("ghost")).thenReturn(Optional.empty());

        assertEquals(ClaimResult.NOT_FOUND, processor.claim("ghost"));
    }

    @Test
    void skippedNotificationIsNeverClaimed() {
        Notification n = queuedNotification("n1");
        n.transitionTo(NotificationStatus.SKIPPED);

        assertEquals(ClaimResult.ALREADY_HANDLED, processor.claim("n1"));
        assertEquals(NotificationStatus.SKIPPED, n.getStatus());
    }

    @Test
    void successMarksSentAndRecordsAttempt() {
        Notification n = queuedNotification("n1");
        processor.claim("n1");

        processor.complete("n1", SendOutcome.success("msg-1"), "mock-email");

        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
        ArgumentCaptor<NotificationAttempt> captor = ArgumentCaptor.forClass(NotificationAttempt.class);
        verify(attempts).save(captor.capture());
        assertEquals(AttemptStatus.SUCCESS, captor.getValue().getStatus());
        assertEquals("msg-1", captor.getValue().getProviderMessageId());
    }

    @Test
    void completeWithoutClaimIsABug() {
        queuedNotification("n1");   // still QUEUED, never claimed

        assertThrows(IllegalStateException.class,
                () -> processor.complete("n1", SendOutcome.success("x"), "mock-email"));
    }

    @Test
    void permanentFailureFailsImmediately() {
        Notification n = queuedNotification("n1");
        processor.claim("n1");

        processor.complete("n1", SendOutcome.permanent("HTTP 400"), "sendgrid");

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals(0, n.getRetryCount());
        assertEquals("HTTP 400", n.getFailureReason());
    }

    @Test
    void retryableFailureGoesBackToQueuedAndWaitsForRepublish() {
        Notification n = queuedNotification("n1");
        processor.claim("n1");

        processor.complete("n1", SendOutcome.retryable("timeout"), "sendgrid");

        assertEquals(NotificationStatus.QUEUED, n.getStatus());
        assertEquals(1, n.getRetryCount());
        assertNull(n.getPublishedAt());
    }

    @Test
    void retriesStopAfterMaxAttempts() {
        Notification n = queuedNotification("n1");

        for (int i = 0; i < NotificationProcessor.MAX_ATTEMPTS; i++) {
            assertEquals(ClaimResult.CLAIMED, processor.claim("n1"));
            processor.complete("n1", SendOutcome.retryable("timeout"), "sendgrid");
        }

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertTrue(n.getFailureReason().startsWith("RETRIES_EXHAUSTED"));
    }
}