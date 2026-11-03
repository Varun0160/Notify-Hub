package com.notifyhub.recipient.dto;

import com.notifyhub.common.enums.Channel;

public record PreferenceResponse(Channel channel, boolean enabled) {
}