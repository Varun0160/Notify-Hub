package com.notifyhub.notification;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByPublicId(String publicId);

    Optional<Notification> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    Optional<Notification> findByPublicIdAndTenantId(String publicId, Long tenantId);

    long countByStatus(NotificationStatus status);

    /**
     * Rows the sweeper should publish. Must run inside a transaction.
     * - unscheduled rows: only after a short grace period (the normal after-commit path goes first)
     * - scheduled rows (also retries and quiet-hours deferrals): as soon as they are due
     * - rows whose lease is still alive belong to another sweeper and are skipped
     * FOR UPDATE SKIP LOCKED: rows locked by a concurrent sweeper are skipped, never waited for.
     */
    @Query(value = "select * from notifications "
            + "where status = 'QUEUED' and published_at is null "
            + "and ((scheduled_at is null and created_at < :olderThan) "
            + "  or (scheduled_at is not null and scheduled_at <= :now)) "
            + "and (lease_until is null or lease_until < :now) "
            + "order by id limit :limit for update skip locked",
            nativeQuery = true)
    List<Notification> lockDueUnpublished(@Param("now") Instant now,
                                          @Param("olderThan") Instant olderThan,
                                          @Param("limit") int limit);

    // REQUIRES_NEW: called from an AFTER_COMMIT listener (Day 10, concept 8). Clears the lease too.
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("update Notification n set n.publishedAt = :at, n.leaseUntil = null where n.id = :id")
    int markPublished(@Param("id") Long id, @Param("at") Instant at);

    /** PROCESSING rows untouched since the cutoff: their worker most likely died. */
    @Query("select n from Notification n "
            + "where n.status = com.notifyhub.common.enums.NotificationStatus.PROCESSING "
            + "and n.updatedAt < :cutoff "
            + "order by n.id")
    List<Notification> findStaleProcessing(@Param("cutoff") Instant cutoff, Pageable pageable);

    /** History with optional filters, newest first, keyset-paginated by id. */
    @Query("select n from Notification n left join fetch n.template "
            + "where n.tenant.id = :tenantId "
            + "and (:beforeId is null or n.id < :beforeId) "
            + "and (:status is null or n.status = :status) "
            + "and (:channel is null or n.channel = :channel) "
            + "and (:recipientId is null or n.recipient.publicId = :recipientId) "
            + "and (:from is null or n.createdAt >= :from) "
            + "and (:to is null or n.createdAt < :to) "
            + "order by n.id desc")
    List<Notification> search(@Param("tenantId") Long tenantId,
                              @Param("beforeId") Long beforeId,
                              @Param("status") NotificationStatus status,
                              @Param("channel") Channel channel,
                              @Param("recipientId") String recipientId,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);
}