package com.notifyhub.webhook;

public enum WebhookResult {
    APPLIED,     // changed a notification's status
    IGNORED,     // valid, but nothing to change (terminal state, opened, unknown type)
    DUPLICATE    // we already processed this event id
}