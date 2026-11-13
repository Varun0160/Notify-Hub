package com.notifyhub.common.exception;

public class StateConflictException extends RuntimeException {
    public StateConflictException(String message) {
        super(message);
    }
}