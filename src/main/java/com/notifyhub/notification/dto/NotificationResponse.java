package com.notifyhub.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.notification.Notification;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        String id,
        Channel channel,
        NotificationStatus status,
        String template,
        Integer templateVersion,
        Instant scheduledAt,
        String reason,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getPublicId(),
                n.getChannel(),
                n.getStatus(),
                n.getTemplate() == null ? null : n.getTemplate().getName(),
                n.getTemplate() == null ? null : n.getTemplate().getVersionNo(),
                n.getScheduledAt(),
                n.getFailureReason(),
                n.getCreatedAt());
    }
}