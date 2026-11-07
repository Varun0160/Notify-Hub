package com.notifyhub.common.enums;

public enum NotificationStatus {
    QUEUED,      // saved, waiting for a worker (or for its scheduled time)
    PROCESSING,  // a worker picked it up
    SENT,        // handed to the provider successfully
    DELIVERED,   // provider confirmed delivery (webhook)
    FAILED,      // retries exhausted, or provider reported failure
    SKIPPED,     // blocked by the recipient's preference
    CANCELLED;   // cancelled by the client before sending

    /** The single place that defines which status changes are legal. */
    public boolean canTransitionTo(NotificationStatus next) {
        return switch (this) {
            case QUEUED -> next == PROCESSING || next == SKIPPED || next == CANCELLED;
            case PROCESSING -> next == SENT || next == FAILED || next == QUEUED; // QUEUED = retry
            case SENT -> next == DELIVERED || next == FAILED;                    // FAILED = bounce
            case DELIVERED, FAILED, SKIPPED, CANCELLED -> false;                 // terminal
        };
    }
}