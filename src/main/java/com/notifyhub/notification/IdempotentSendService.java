package com.notifyhub.notification;

import com.notifyhub.notification.dto.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotentSendService {

    private final NotificationService notificationService;

    // Deliberately NOT @Transactional: each call below gets its own transaction.
    public SendResult send(Long tenantId, SendNotificationRequest request, String idempotencyKey) {
        String key = IdempotencyKeys.normalize(idempotencyKey);
        try {
            return notificationService.accept(tenantId, request, key);
        } catch (DataIntegrityViolationException ex) {
            if (key == null) {
                throw ex;                      // not an idempotency problem
            }
            // We lost a race: another request with the same key committed first.
            // The failed transaction is gone, so read the winner's row in a fresh one.
            return notificationService.replay(tenantId, key, request)
                    .orElseThrow(() -> ex);    // no such row: it was some other constraint
        }
    }
}