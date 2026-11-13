package com.notifyhub.observability;

import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.notification.NotificationRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BacklogGaugesTest {

    // A field, not a local: a gauge holds only a WEAK reference, so an unreferenced object could be
    // garbage-collected mid-test and the gauge would read NaN. In production Spring keeps the bean alive.
    private BacklogGauges gauges;

    @Test
    void gaugesReadTheCurrentCountsFromTheDatabase() {
        NotificationRepository repository = mock(NotificationRepository.class);
        when(repository.countByStatus(NotificationStatus.QUEUED)).thenReturn(7L);
        when(repository.countByStatus(NotificationStatus.PROCESSING)).thenReturn(2L);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        gauges = new BacklogGauges(repository, registry);

        assertEquals(7.0, registry.get("notifyhub.notifications.backlog").tag("status", "queued").gauge().value());
        assertEquals(2.0, registry.get("notifyhub.notifications.backlog").tag("status", "processing").gauge().value());

        when(repository.countByStatus(NotificationStatus.QUEUED)).thenReturn(0L);   // the gauge is live
        assertEquals(0.0, registry.get("notifyhub.notifications.backlog").tag("status", "queued").gauge().value());
    }
}