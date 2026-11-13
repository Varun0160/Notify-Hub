package com.notifyhub.notification;

import com.notifyhub.common.PageResponse;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.notification.dto.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationQueryServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationQueryService service = new NotificationQueryService(repository);

    private Notification withId(long id) {
        Notification n = new Notification(null, null, null, Channel.EMAIL);
        ReflectionTestUtils.setField(n, "id", id);
        return n;
    }

    private PageResponse<NotificationResponse> list(Integer limit, String cursor) {
        return service.list(1L, null, null, null, null, null, limit, cursor);
    }

    @Test
    void anExtraRowMeansThereIsANextPageAndTheCursorPointsAtTheLastReturnedRow() {
        when(repository.search(eq(1L), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(withId(30), withId(29), withId(28)));

        PageResponse<NotificationResponse> page = list(2, null);

        assertEquals(2, page.items().size());
        assertEquals(29L, PageCursor.decode(page.nextCursor()));
    }

    @Test
    void theLastPageHasNoCursor() {
        when(repository.search(eq(1L), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(withId(2), withId(1)));

        PageResponse<NotificationResponse> page = list(5, null);

        assertEquals(2, page.items().size());
        assertNull(page.nextCursor());
    }

    @Test
    void itAsksTheDatabaseForOneRowMoreThanTheLimit() {
        when(repository.search(eq(1L), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        list(10, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(eq(1L), any(), any(), any(), any(), any(), any(), captor.capture());
        assertEquals(11, captor.getValue().getPageSize());
    }

    @Test
    void theCursorIsPassedAsTheUpperBound() {
        when(repository.search(eq(1L), eq(29L), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(withId(28)));

        assertEquals(1, list(10, PageCursor.encode(29)).items().size());
    }

    @Test
    void badLimitsAreRejectedBeforeTouchingTheDatabase() {
        assertThrows(InvalidRequestException.class, () -> list(0, null));
        assertThrows(InvalidRequestException.class, () -> list(101, null));
        verifyNoInteractions(repository);
    }

    @Test
    void anInvalidCursorOrDateRangeIsRejected() {
        assertThrows(InvalidRequestException.class, () -> list(10, "garbage!!"));
        assertThrows(InvalidRequestException.class, () -> service.list(1L, null, null, null,
                Instant.parse("2026-12-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"), 10, null));
    }
}