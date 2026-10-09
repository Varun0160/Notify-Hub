package com.hookrelay.hookrelay.auth;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {}
