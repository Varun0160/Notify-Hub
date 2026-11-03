package com.notifyhub.security;

public record UserPrincipal(String userPublicId, String email, String role) {
}