package com.notifyhub.webhook;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationAttempt;
import com.notifyhub.notification.NotificationAttemptRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookServiceTest {

    private final NotificationAttemptRepository attempts = mock(NotificationAttemptRepository.class);
    private final WebhookEventRepository events = mock(WebhookEventRepository.class);
    private final WebhookService service = new WebhookService(attempts, events);

    /** A notification that was sent successfully, with the attempt that carries the provider id. */
    private Notification sentNotification(String messageId) {
        Notification n = new Notification(null, null, null, Channel.EMAIL);
        n.transitionTo(NotificationStatus.PROCESSING);
        n.transitionTo(NotificationStatus.SENT);
        NotificationAttempt attempt = new NotificationAttempt(n, 1, "mock-email");
        attempt.setProviderMessageId(messageId);
        when(attempts.findByProviderMessageId(messageId)).thenReturn(Optional.of(attempt));
        return n;
    }

    private WebhookPayload event(String id, String messageId, String type) {
        return new WebhookPayload(id, messageId, type);
    }

    @Test
    void deliveredMovesSentToDelivered() {
        Notification n = sentNotification("m1");

        assertEquals(WebhookResult.APPLIED, service.handle(event("e1", "m1", "delivered")));

        assertEquals(NotificationStatus.DELIVERED, n.getStatus());
        verify(events).save(any(WebhookEvent.class));
    }

    @Test
    void bounceFailsTheNotificationWithAReason() {
        Notification n = sentNotification("m1");

        assertEquals(WebhookResult.APPLIED, service.handle(event("e1", "m1", "bounced")));

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals("BOUNCED", n.getFailureReason());
    }

    @Test
    void anAlreadyProcessedEventIdIsADuplicateAndTouchesNothing() {
        when(events.existsBySourceAndEventId(WebhookService.SOURCE, "e1")).thenReturn(true);

        assertEquals(WebhookResult.DUPLICATE, service.handle(event("e1", "m1", "delivered")));

        verify(attempts, never()).findByProviderMessageId(anyString());
        verify(events, never()).save(any());
    }

    @Test
    void unknownMessageIdIsNotFoundAndNothingIsRecorded() {
        when(attempts.findByProviderMessageId("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.handle(event("e1", "ghost", "delivered")));

        verify(events, never()).save(any());   // so the provider's retry is processed normally
    }

    @Test
    void aSecondDeliveredEventWithAnotherIdIsIgnored() {
        Notification n = sentNotification("m1");
        service.handle(event("e1", "m1", "delivered"));

        assertEquals(WebhookResult.IGNORED, service.handle(event("e2", "m1", "delivered")));
        assertEquals(NotificationStatus.DELIVERED, n.getStatus());
    }

    @Test
    void aBounceAfterDeliveryIsIgnoredBecauseDeliveredIsTerminal() {
        Notification n = sentNotification("m1");
        service.handle(event("e1", "m1", "delivered"));

        assertEquals(WebhookResult.IGNORED, service.handle(event("e2", "m1", "bounced")));
        assertEquals(NotificationStatus.DELIVERED, n.getStatus());
    }

    @Test
    void aDeliveredEventAfterABounceIsIgnored() {
        Notification n = sentNotification("m1");
        service.handle(event("e1", "m1", "bounced"));

        assertEquals(WebhookResult.IGNORED, service.handle(event("e2", "m1", "delivered")));
        assertEquals(NotificationStatus.FAILED, n.getStatus());
    }

    @Test
    void openedEventsAreRecordedButChangeNothing() {
        Notification n = sentNotification("m1");

        assertEquals(WebhookResult.IGNORED, service.handle(event("e1", "m1", "opened")));

        assertEquals(NotificationStatus.SENT, n.getStatus());
        verify(events).save(any(WebhookEvent.class));
    }

    @Test
    void unknownEventTypesAreAcknowledgedNotRejected() {
        sentNotification("m1");

        assertEquals(WebhookResult.IGNORED, service.handle(event("e1", "m1", "something_new")));
    }

    @Test
    void theRecordedEventCarriesTheOutcome() {
        sentNotification("m1");

        service.handle(event("e1", "m1", "delivered"));

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(events).save(captor.capture());
        assertEquals("e1", captor.getValue().getEventId());
        assertEquals("APPLIED", captor.getValue().getOutcome());
    }
}