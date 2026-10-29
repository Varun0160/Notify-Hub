package com.notifyhub.notification;

import com.notifyhub.common.BaseEntity;
import com.notifyhub.common.enums.AttemptStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_attempts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false)
    private int attemptNo;

    @Column(nullable = false, length = 50)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptStatus status;

    @Column(length = 255)
    private String providerMessageId;

    @Column(length = 1000)
    private String errorMessage;

    public NotificationAttempt(Notification notification, int attemptNo, String provider) {
        this.notification = notification;
        this.attemptNo = attemptNo;
        this.provider = provider;
    }
}