package com.notifyhub.webhook;

import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.common.exception.InvalidSignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final int MAX_BODY_CHARS = 10_000;

    private final WebhookProperties props;
    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping("/provider")
    public Map<String, String> receive(
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String rawBody) {

        if (rawBody.length() > MAX_BODY_CHARS) {
            throw new InvalidRequestException("Webhook body is too large");
        }

        // 1. Verify FIRST, over the exact bytes we received
        if (!WebhookSignature.isValid(props.secret(), timestamp, signature, rawBody,
                Instant.now(), props.toleranceSeconds())) {
            log.warn("Rejected webhook with an invalid signature");
            throw new InvalidSignatureException("Invalid webhook signature");
        }

        // 2. Only now trust the content enough to parse it
        WebhookPayload payload = WebhookPayload.parse(objectMapper, rawBody);

        // 3. Apply it
        WebhookResult result = webhookService.handle(payload);
        return Map.of("result", result.name());
    }
}