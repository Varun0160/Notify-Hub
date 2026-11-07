package com.notifyhub.notification;

import com.notifyhub.common.BaseEntity;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.recipient.Recipient;
import com.notifyhub.template.Template;
import com.notifyhub.tenant.Tenant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Column(nullable = false, updatable = false, length = 36)
    private String publicId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    // No setter: the only way to change status is transitionTo(...)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Setter
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Setter
    @Column(length = 100)
    private String idempotencyKey;

    @Setter
    @Column(length = 64)
    private String requestHash;

    @Setter
    private Instant scheduledAt;

    @Setter
    private Instant sentAt;

    @Setter
    @Column(nullable = false)
    private int retryCount = 0;

    @Setter
    @Column(length = 500)
    private String failureReason;

    @Version
    private Long version;

    public Notification(Tenant tenant, Recipient recipient, Template template, Channel channel) {
        this.tenant = tenant;
        this.recipient = recipient;
        this.template = template;
        this.channel = channel;
    }

    public void transitionTo(NotificationStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new IllegalStateException("Illegal status change: " + status + " -> " + next);
        }
        this.status = next;
    }
}