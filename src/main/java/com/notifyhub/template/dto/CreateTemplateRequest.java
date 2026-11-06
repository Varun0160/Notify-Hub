package com.notifyhub.template.dto;

import com.notifyhub.common.enums.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTemplateRequest(
        @NotBlank(message = "name is required")
        @Pattern(regexp = "^[a-z0-9][a-z0-9_-]{1,99}$",
                message = "name must be 2-100 chars: lowercase letters, digits, '_' or '-'")
        String name,

        @NotNull(message = "channel is required")
        Channel channel,

        @Size(max = 255, message = "subject must be at most 255 characters")
        String subject,

        @NotBlank(message = "body is required")
        @Size(max = 10000, message = "body must be at most 10000 characters")
        String body
) {
}