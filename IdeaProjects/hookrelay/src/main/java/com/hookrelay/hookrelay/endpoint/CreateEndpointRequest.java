package com.hookrelay.hookrelay.endpoint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateEndpointRequest(
        @NotBlank @Size(max = 500)
        @Pattern(regexp = "^https?://.+", message = "must start with http:// or https://")
        String url,

        @NotEmpty Set<@NotBlank @Size(max = 100) String> eventTypes
) {}