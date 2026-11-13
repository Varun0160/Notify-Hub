package com.notifyhub.webhook;

import com.notifyhub.common.exception.InvalidRequestException;
import tools.jackson.databind.ObjectMapper;

public record WebhookPayload(String eventId, String providerMessageId, String type) {

    /** Called only AFTER the signature has been verified. */
    public static WebhookPayload parse(ObjectMapper objectMapper, String rawBody) {
        WebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, WebhookPayload.class);
        } catch (RuntimeException ex) {
            throw new InvalidRequestException("Webhook body is not valid JSON");
        }
        if (payload == null
                || blank(payload.eventId()) || payload.eventId().length() > 100
                || blank(payload.providerMessageId()) || payload.providerMessageId().length() > 255
                || blank(payload.type()) || payload.type().length() > 30) {
            throw new InvalidRequestException("Webhook needs eventId, providerMessageId and type");
        }
        return payload;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}