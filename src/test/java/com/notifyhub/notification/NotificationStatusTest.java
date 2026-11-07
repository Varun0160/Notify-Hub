package com.notifyhub.notification;

import com.notifyhub.common.enums.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.notifyhub.common.enums.NotificationStatus.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationStatusTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(QUEUED.canTransitionTo(PROCESSING));
        assertTrue(PROCESSING.canTransitionTo(SENT));
        assertTrue(SENT.canTransitionTo(DELIVERED));
    }

    @Test
    void retryReturnsToQueued() {
        assertTrue(PROCESSING.canTransitionTo(QUEUED));
        assertTrue(PROCESSING.canTransitionTo(FAILED));
    }

    @Test
    void optOutAndCancelHappenOnlyFromQueued() {
        assertTrue(QUEUED.canTransitionTo(SKIPPED));
        assertTrue(QUEUED.canTransitionTo(CANCELLED));
        assertFalse(SENT.canTransitionTo(CANCELLED));
        assertFalse(PROCESSING.canTransitionTo(SKIPPED));
    }

    @Test
    void cannotSkipSteps() {
        assertFalse(QUEUED.canTransitionTo(SENT));
        assertFalse(QUEUED.canTransitionTo(DELIVERED));
        assertFalse(PROCESSING.canTransitionTo(DELIVERED));
    }

    @Test
    void terminalStatesGoNowhere() {
        for (NotificationStatus terminal : List.of(DELIVERED, FAILED, SKIPPED, CANCELLED)) {
            for (NotificationStatus target : NotificationStatus.values()) {
                assertFalse(terminal.canTransitionTo(target), terminal + " -> " + target);
            }
        }
    }
}