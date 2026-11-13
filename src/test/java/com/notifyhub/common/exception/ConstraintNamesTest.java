package com.notifyhub.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ConstraintNamesTest {

    @Test
    void duplicateKeyGivesTheConstraintNameButNotTheValue() {
        Exception ex = new RuntimeException("could not execute statement",
                new RuntimeException("Duplicate entry 'ravi@example.com' for key 'recipients.uk_recipients_tenant_external'"));

        String name = ConstraintNames.from(ex);

        assertEquals("recipients.uk_recipients_tenant_external", name);
        assertFalse(name.contains("ravi"));
    }

    @Test
    void aMaliciousValueCannotImpersonateTheConstraintName() {
        // The attacker controls the value between the first pair of quotes
        Exception ex = new RuntimeException(
                "Duplicate entry 'x' for key 'evil' for key 'notifications.uk_notifications_idempotency'");

        assertEquals("notifications.uk_notifications_idempotency", ConstraintNames.from(ex));
    }

    @Test
    void anAttackerValueWithUnsafeCharactersIsNeverReturned() {
        Exception ex = new RuntimeException("Duplicate entry 'a' for key 'x\nINJECTED LOG LINE'");

        assertEquals("unknown", ConstraintNames.from(ex));
    }

    @Test
    void foreignKeyFailuresGiveTheConstraintName() {
        Exception ex = new RuntimeException("Cannot add or update a child row: a foreign key constraint fails "
                + "(`notifyhub`.`notifications`, CONSTRAINT `fk_notifications_recipient` FOREIGN KEY (`recipient_id`) "
                + "REFERENCES `recipients` (`id`))");

        assertEquals("fk_notifications_recipient", ConstraintNames.from(ex));
    }

    @Test
    void unrecognisedOrMissingMessagesGiveUnknown() {
        assertEquals("unknown", ConstraintNames.from(new RuntimeException((String) null)));
        assertEquals("unknown", ConstraintNames.from(new RuntimeException("something else")));
    }
}