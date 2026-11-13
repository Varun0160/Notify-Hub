package com.notifyhub.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notifyhub.notification.SendResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchItemResult(
        int index,
        String result,                 // ACCEPTED or ERROR
        int httpStatus,                // what a single call would have returned
        NotificationResponse notification,
        Boolean replayed,
        String error,
        Long retryAfterSeconds
) {
    public static BatchItemResult accepted(int index, SendResult r) {
        return new BatchItemResult(index, "ACCEPTED", 202, r.response(), r.replayed() ? true : null, null, null);
    }

    public static BatchItemResult error(int index, int httpStatus, String message, Long retryAfterSeconds) {
        return new BatchItemResult(index, "ERROR", httpStatus, null, null, message, retryAfterSeconds);
    }
}