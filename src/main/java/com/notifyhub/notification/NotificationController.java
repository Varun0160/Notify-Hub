package com.notifyhub.notification;

import com.notifyhub.common.PageResponse;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.notification.dto.BatchSendRequest;
import com.notifyhub.notification.dto.BatchSendResponse;
import com.notifyhub.notification.dto.NotificationResponse;
import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.security.TenantPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final IdempotentSendService idempotentSendService;
    private final NotificationService notificationService;
    private final NotificationQueryService queryService;
    private final BatchSendService batchSendService;

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

    @PostMapping("/batch")
    public ResponseEntity<BatchSendResponse> sendBatch(
            @AuthenticationPrincipal TenantPrincipal principal,
            @Valid @RequestBody BatchSendRequest request) {

        BatchSendResponse response = batchSendService.send(principal.tenantId(), request);
        HttpStatus status = (response.failed() == 0) ? HttpStatus.ACCEPTED : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal TenantPrincipal principal,
            @RequestParam(value = "status", required = false) NotificationStatus status,
            @RequestParam(value = "channel", required = false) Channel channel,
            @RequestParam(value = "recipientId", required = false) String recipientId,
            @RequestParam(value = "from", required = false) Instant from,
            @RequestParam(value = "to", required = false) Instant to,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor) {

        return queryService.list(principal.tenantId(), status, channel, recipientId, from, to, limit, cursor);
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@AuthenticationPrincipal TenantPrincipal principal,
                                    @PathVariable("id") String id) {
        return notificationService.get(principal.tenantId(), id);
    }

    @PostMapping("/{id}/cancel")
    public NotificationResponse cancel(@AuthenticationPrincipal TenantPrincipal principal,
                                       @PathVariable("id") String id) {
        return notificationService.cancel(principal.tenantId(), id);
    }
}