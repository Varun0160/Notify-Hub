package com.notifyhub.common.enums;

public enum NotificationStatus {
    QUEUED,      // saved, waiting to be processed
    PROCESSING,  // a worker picked it up
    SENT,        // handed to the provider
    DELIVERED,   // provider confirmed delivery (webhook)
    FAILED,      // retries exhausted
    SKIPPED,     // blocked by preference or rate limit
    CANCELLED
}
