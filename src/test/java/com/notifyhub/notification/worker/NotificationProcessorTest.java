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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationProcessorTest {

    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
    private final RetryPolicy policy = new RetryPolicy(3, Duration.ofSeconds(1), Duration.ofSeconds(10));
    private final NotificationProcessor processor =
            new NotificationProcessor(notifications, attempts, JsonMapper.builder().build(), policy);

    private Notification queuedNotification(String id) {
        Notification n = new Notification(null, null, null, Channel.EMAIL);
        when(notifications.findByPublicId(id)).thenReturn(Optional.of(n));
        when(notifications.findById(1L)).thenReturn(Optional.of(n));
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
        queuedNotification("n1");

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
    void retryableFailureIsScheduledForLater() {
        Notification n = queuedNotification("n1");
        processor.claim("n1");
        Instant before = Instant.now();

        processor.complete("n1", SendOutcome.retryable("timeout"), "sendgrid");

        assertEquals(NotificationStatus.QUEUED, n.getStatus());
        assertEquals(1, n.getRetryCount());
        assertNull(n.getPublishedAt());
        assertTrue(n.getScheduledAt().isAfter(before), "retry must not be due immediately");
    }

    @Test
    void secondRetryWaitsLongerThanTheFirst() {
        Notification n = queuedNotification("n1");
        processor.claim("n1");
        processor.complete("n1", SendOutcome.retryable("timeout"), "sendgrid");
        processor.claim("n1");
        Instant before = Instant.now();

        processor.complete("n1", SendOutcome.retryable("timeout"), "sendgrid");

        // second failure: window is 1 s to 2 s, so at least ~1 s from now
        assertTrue(n.getScheduledAt().isAfter(before.plusMillis(900)));
    }

    @Test
    void retriesStopAfterMaxAttempts() {
        Notification n = queuedNotification("n1");

        for (int i = 0; i < policy.maxAttempts(); i++) {
            assertEquals(ClaimResult.CLAIMED, processor.claim("n1"));
            processor.complete("n1", SendOutcome.retryable("timeout"), "sendgrid");
        }

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertTrue(n.getFailureReason().startsWith("RETRIES_EXHAUSTED"));
    }

    @Test
    void staleProcessingRowIsRequeuedWithBackoffAndAnAttemptRecord() {
        Notification n = queuedNotification("n1");
        processor.claim("n1");

        assertTrue(processor.recoverStale(1L));

        assertEquals(NotificationStatus.QUEUED, n.getStatus());
        assertEquals(1, n.getRetryCount());
        assertNotNull(n.getScheduledAt());
        ArgumentCaptor<NotificationAttempt> captor = ArgumentCaptor.forClass(NotificationAttempt.class);
        verify(attempts).save(captor.capture());
        assertEquals("reaper", captor.getValue().getProvider());
        assertEquals(AttemptStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void rowThatFinishedMeanwhileIsLeftAlone() {
        Notification n = queuedNotification("n1");
        processor.claim("n1");
        processor.complete("n1", SendOutcome.success("m"), "mock-email");

        assertFalse(processor.recoverStale(1L));
        assertEquals(NotificationStatus.SENT, n.getStatus());
    }

    @Test
    void repeatedCrashesEndInFailedNotAnEndlessLoop() {
        Notification n = queuedNotification("n1");

        for (int i = 0; i < policy.maxAttempts(); i++) {
            processor.claim("n1");
            processor.recoverStale(1L);
        }

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertTrue(n.getFailureReason().startsWith("RETRIES_EXHAUSTED"));
    }

    @Test
    void unknownIdIsIgnoredByTheReaper() {
        when(notifications.findById(99L)).thenReturn(Optional.empty());

        assertFalse(processor.recoverStale(99L));
        verify(attempts, never()).save(any());
    }
}