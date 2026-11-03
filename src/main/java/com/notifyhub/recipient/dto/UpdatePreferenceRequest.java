package com.notifyhub.recipient.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePreferenceRequest(
        @NotNull(message = "enabled is required") Boolean enabled
) {
}