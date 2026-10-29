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
@Setter
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.QUEUED;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(length = 100)
    private String idempotencyKey;

    private Instant scheduledAt;
    private Instant sentAt;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(length = 500)
    private String failureReason;

    @Version
    private Long version;
}