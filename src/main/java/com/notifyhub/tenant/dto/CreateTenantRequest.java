package com.notifyhub.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank(message = "name is required")
        @Size(min = 3, max = 100, message = "name must be between 3 and 100 characters")
        String name
) {
}