package com.notifyhub.notification;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.common.exception.IdempotencyConflictException;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.common.exception.RateLimitExceededException;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.notification.dto.BatchItemResult;
import com.notifyhub.notification.dto.BatchSendRequest;
import com.notifyhub.notification.dto.BatchSendResponse;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchSendServiceTest {

    private final IdempotentSendService sender = mock(IdempotentSendService.class);
    private final BatchSendService service = new BatchSendService(sender);

    private BatchSendRequest.Item item() {
        return new BatchSendRequest.Item(null,
                new SendNotificationRequest("r1", Channel.EMAIL, "welcome", Map.of(), null, null));
    }

    private BatchSendRequest batchOf(int n) {
        return new BatchSendRequest(java.util.stream.IntStream.range(0, n).mapToObj(i -> item()).toList());
    }

    private SendResult ok(boolean replayed) {
        return new SendResult(new NotificationResponse("n1", Channel.EMAIL, NotificationStatus.QUEUED,
                "welcome", 1, null, null, Instant.now()), replayed);
    }

    @Test
    void allAcceptedGivesZeroFailures() {
        when(sender.send(eq(1L), any(), any())).thenReturn(ok(false));

        BatchSendResponse response = service.send(1L, batchOf(3));

        assertEquals(3, response.total());
        assertEquals(3, response.accepted());
        assertEquals(0, response.failed());
    }

    @Test
    void oneBadItemDoesNotStopTheOthersAndEachFailureKeepsItsOwnStatus() {
        when(sender.send(eq(1L), any(), any()))
                .thenReturn(ok(false))
                .thenThrow(new ResourceNotFoundException("Recipient not found: x"))
                .thenThrow(new InvalidRequestException("Missing template variables: [name]"))
                .thenThrow(new RateLimitExceededException("slow down", 30))
                .thenThrow(new IdempotencyConflictException("key reused"))
                .thenReturn(ok(false));

        BatchSendResponse response = service.send(1L, batchOf(6));

        assertEquals(2, response.accepted());
        assertEquals(4, response.failed());
        List<BatchItemResult> r = response.results();
        for (int i = 0; i < 6; i++) {
            assertEquals(i, r.get(i).index());
        }
        assertEquals(404, r.get(1).httpStatus());
        assertEquals(400, r.get(2).httpStatus());
        assertEquals(429, r.get(3).httpStatus());
        assertEquals(30L, r.get(3).retryAfterSeconds());
        assertEquals(422, r.get(4).httpStatus());
        assertEquals("ACCEPTED", r.get(5).result());
    }

    @Test
    void unexpectedErrorsBecome500WithoutLeakingDetails() {
        when(sender.send(eq(1L), any(), any())).thenThrow(new RuntimeException("jdbc:mysql://secret-host"));

        BatchItemResult result = service.send(1L, batchOf(1)).results().get(0);

        assertEquals(500, result.httpStatus());
        assertFalse(result.error().contains("secret"));
    }

    @Test
    void replaysAreFlaggedAndNormalAcceptsAreNot() {
        when(sender.send(eq(1L), any(), any())).thenReturn(ok(true)).thenReturn(ok(false));

        List<BatchItemResult> results = service.send(1L, batchOf(2)).results();

        assertTrue(results.get(0).replayed());
        assertNull(results.get(1).replayed());
    }
}