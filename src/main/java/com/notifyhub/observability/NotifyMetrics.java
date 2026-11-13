package com.notifyhub.observability;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.provider.SendOutcome;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/** One place that names every business metric. Tag values come from small, fixed sets only. */
@Component
public class NotifyMetrics {

    private final MeterRegistry registry;


    public NotifyMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** A send request was accepted (QUEUED) or recorded as SKIPPED. */
    public void accepted(Channel channel, NotificationStatus status) {
        registry.counter("notifyhub.notifications.accepted",
                "channel", lower(channel.name()), "status", lower(status.name())).increment();
    }

    /** What the worker concluded about one delivery attempt. */
    public void outcome(String channel, SendOutcome.Status status) {
        String outcome = switch (status) {
            case SUCCESS -> "sent";
            case RETRYABLE_FAILURE -> "retry";
            case PERMANENT_FAILURE -> "failed";
        };
        registry.counter("notifyhub.notifications.outcome",
                "channel", lower(channel), "outcome", outcome).increment();
    }

    public void providerCall(String provider, SendOutcome.Status status, Duration took) {
        registry.timer("notifyhub.provider.latency",
                "provider", provider, "outcome", lower(status.name())).record(took);
    }

    /** result: applied, ignored, duplicate or rejected. */
    public void webhook(String result) {
        registry.counter("notifyhub.webhooks.received", "result", result).increment();
    }

    private static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}