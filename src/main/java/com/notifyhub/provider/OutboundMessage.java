package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;

/** A fully rendered message, ready for a provider. `to` is an email, phone number or device token. */
public record OutboundMessage(Channel channel, String to, String subject, String body) {
}