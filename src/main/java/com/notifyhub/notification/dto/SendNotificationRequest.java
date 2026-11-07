package com.notifyhub.notification.dto;

import com.notifyhub.common.enums.Channel;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record SendNotificationRequest(
        @NotBlank(message = "recipientId is required")
        String recipientId,

        @NotNull(message = "channel is required")
        Channel channel,

        @NotBlank(message = "template is required")
        @Size(max = 100, message = "template must be at most 100 characters")
        String template,

        @Size(max = 50, message = "at most 50 variables allowed")
        Map<String, String> variables,

        @Future(message = "scheduledAt must be in the future")
        Instant scheduledAt,

        Boolean urgent
) {
}