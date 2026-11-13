package com.notifyhub.notification.queue;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SweepClaimer {

    /** Plain data: nothing lazy escapes the transaction. */
    public record Claimed(Long id, String publicId, Channel channel) {
    }

    private final NotificationRepository notificationRepository;

    @Transactional
    public List<Claimed> claimBatch(Instant now, Instant graceCutoff, int limit, Duration lease) {
        List<Notification> rows = notificationRepository.lockDueUnpublished(now, graceCutoff, limit);
        Instant leaseUntil = now.plus(lease);
        for (Notification row : rows) {
            row.setLeaseUntil(leaseUntil);        // dirty checking writes it at commit
        }
        return rows.stream()
                .map(n -> new Claimed(n.getId(), n.getPublicId(), n.getChannel()))
                .toList();
    }
}