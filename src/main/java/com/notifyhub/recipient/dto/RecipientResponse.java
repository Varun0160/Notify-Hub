package com.notifyhub.recipient.dto;

import com.notifyhub.recipient.Recipient;

import java.time.Instant;
import java.time.LocalTime;

public record RecipientResponse(
        String id,
        String externalId,
        String email,
        String phone,
        String deviceToken,
        String timezone,
        LocalTime quietHoursStart,
        LocalTime quietHoursEnd,
        Instant createdAt
) {
    public static RecipientResponse from(Recipient r) {
        return new RecipientResponse(
                r.getPublicId(), r.getExternalId(), r.getEmail(), r.getPhone(),
                r.getDeviceToken(), r.getTimezone(),
                r.getQuietHoursStart(), r.getQuietHoursEnd(), r.getCreatedAt());
    }
}