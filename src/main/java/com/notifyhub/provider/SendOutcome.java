package com.notifyhub.provider;

public record SendOutcome(Status status, String providerMessageId, String error) {

    public enum Status { SUCCESS, RETRYABLE_FAILURE, PERMANENT_FAILURE }

    public static SendOutcome success(String providerMessageId) {
        return new SendOutcome(Status.SUCCESS, providerMessageId, null);
    }

    public static SendOutcome retryable(String error) {
        return new SendOutcome(Status.RETRYABLE_FAILURE, null, error);
    }

    public static SendOutcome permanent(String error) {
        return new SendOutcome(Status.PERMANENT_FAILURE, null, error);
    }
}