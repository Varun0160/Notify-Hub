package com.notifyhub.notification;

import com.notifyhub.notification.dto.NotificationResponse;

public record SendResult(NotificationResponse response, boolean replayed) {
}