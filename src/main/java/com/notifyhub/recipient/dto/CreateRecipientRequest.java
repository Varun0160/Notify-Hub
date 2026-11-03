package com.notifyhub.recipient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record CreateRecipientRequest(
        @NotBlank(message = "externalId is required")
        @Size(max = 100, message = "externalId must be at most 100 characters")
        String externalId,

        @Email(message = "email is invalid")
        @Size(max = 255, message = "email must be at most 255 characters")
        String email,

        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$",
                message = "phone must be in E.164 format, e.g. +919876543210")
        String phone,

        @Size(max = 255, message = "deviceToken must be at most 255 characters")
        String deviceToken,

        @Size(max = 50, message = "timezone must be at most 50 characters")
        String timezone,

        LocalTime quietHoursStart,
        LocalTime quietHoursEnd
) {
}