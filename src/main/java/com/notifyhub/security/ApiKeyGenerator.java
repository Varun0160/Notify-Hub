package com.notifyhub.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class ApiKeyGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String KEY_PREFIX = "nh_";

    private ApiKeyGenerator() {
    }

    public static GeneratedKey generate() {
        byte[] bytes = new byte[32];                       // 256 bits of randomness
        RANDOM.nextBytes(bytes);
        String rawKey = KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new GeneratedKey(rawKey, rawKey.substring(0, 12), hash(rawKey));
    }

    public static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);       // 64 hex characters
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record GeneratedKey(String rawKey, String prefix, String hash) {
    }
}