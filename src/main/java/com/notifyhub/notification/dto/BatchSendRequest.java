package com.notifyhub.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchSendRequest(
        @NotEmpty(message = "items must not be empty")
        @Size(max = 100, message = "at most 100 items per batch")
        List<@NotNull(message = "item must not be null") @Valid Item> items
) {

    public record Item(
            String idempotencyKey,
            @NotNull(message = "notification is required") @Valid SendNotificationRequest notification
    ) {
    }
}