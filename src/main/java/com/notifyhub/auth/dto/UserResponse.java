package com.notifyhub.auth.dto;

import com.notifyhub.auth.User;

import java.time.Instant;

public record UserResponse(String id, String email, String fullName, String role,
                           boolean enabled, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getPublicId(), user.getEmail(), user.getFullName(),
                user.getRole().name(), user.isEnabled(), user.getCreatedAt());
    }
}