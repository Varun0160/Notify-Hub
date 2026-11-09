package com.notifyhub.notification;

import com.notifyhub.common.enums.Channel;

public record NotificationQueuedEvent(Long id, String publicId, Channel channel) {
}