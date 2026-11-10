package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;

public interface NotificationProvider {

    Channel channel();

    /** Stored on every attempt row, so we can tell providers apart later. */
    String name();

    /** Must never throw: turn every problem into a SendOutcome. */
    SendOutcome send(OutboundMessage message);
}