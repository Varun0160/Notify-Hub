package com.notifyhub.notification;

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

    /** QUEUED, due, never handed to the broker, and old enough that the normal path must have failed. */
    @Query("select n from Notification n "
            + "where n.status = com.notifyhub.common.enums.NotificationStatus.QUEUED "
            + "and n.publishedAt is null "
            + "and (n.scheduledAt is null or n.scheduledAt <= :now) "
            + "and n.createdAt < :olderThan "
            + "order by n.id")
    List<Notification> findUnpublished(@Param("now") Instant now,
                                       @Param("olderThan") Instant olderThan,
                                       Pageable pageable);

    // REQUIRES_NEW is essential: this is called from an AFTER_COMMIT listener (see concept 8).
    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("update Notification n set n.publishedAt = :at where n.id = :id")
    int markPublished(@Param("id") Long id, @Param("at") Instant at);

    /** PROCESSING rows untouched since the cutoff: their worker most likely died. */
    @Query("select n from Notification n "
            + "where n.status = com.notifyhub.common.enums.NotificationStatus.PROCESSING "
            + "and n.updatedAt < :cutoff "
            + "order by n.id")
    List<Notification> findStaleProcessing(@Param("cutoff") Instant cutoff, Pageable pageable);
}