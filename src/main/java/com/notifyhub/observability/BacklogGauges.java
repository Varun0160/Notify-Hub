package com.notifyhub.observability;

import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.notification.NotificationRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;

/** Live queue sizes. Rising QUEUED = workers too slow. Rising PROCESSING = workers stuck (the reaper's job). */
@Component
public class BacklogGauges {

    private final NotificationRepository repository;

    public BacklogGauges(NotificationRepository repository, MeterRegistry registry) {
        this.repository = repository;
        for (NotificationStatus status : List.of(NotificationStatus.QUEUED, NotificationStatus.PROCESSING)) {
            // The gauge calls this lambda each time metrics are read, and holds only a WEAK reference to `this`.
            Gauge.builder("notifyhub.notifications.backlog", this, g -> g.count(status))
                    .tag("status", status.name().toLowerCase())
                    .register(registry);
        }
    }

    double count(NotificationStatus status) {
        return repository.countByStatus(status);
    }
}