package com.notifyhub.notification.worker;

public enum ClaimResult {
    CLAIMED,          // we own it now: go and send
    NOT_FOUND,        // no such notification
    ALREADY_HANDLED   // not QUEUED anymore: a duplicate or stale message
}