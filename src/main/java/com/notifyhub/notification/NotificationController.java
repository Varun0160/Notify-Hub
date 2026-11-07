package com.notifyhub.notification;

import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.security.TenantPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final IdempotentSendService idempotentSendService;
    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> send(
            @AuthenticationPrincipal TenantPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SendNotificationRequest request) {

        SendResult result = idempotentSendService.send(principal.tenantId(), request, idempotencyKey);

        ResponseEntity.BodyBuilder builder = ResponseEntity.accepted()
                .location(URI.create("/api/v1/notifications/" + result.response().id()));
        if (result.replayed()) {
            builder.header("Idempotent-Replayed", "true");
        }
        return builder.body(result.response());
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@AuthenticationPrincipal TenantPrincipal principal,
                                    @PathVariable("id") String id) {
        return notificationService.get(principal.tenantId(), id);
    }
}