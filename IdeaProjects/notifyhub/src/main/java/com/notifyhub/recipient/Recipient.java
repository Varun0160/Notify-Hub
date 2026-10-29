package com.notifyhub.recipient;

import com.notifyhub.common.BaseEntity;
import com.notifyhub.tenant.Tenant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "recipients")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipient extends BaseEntity {

    @Column(nullable = false, updatable = false, length = 36)
    private String publicId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String externalId;

    @Column(length = 255)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String deviceToken;

    @Column(nullable = false, length = 50)
    private String timezone = "UTC";

    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;

    public Recipient(Tenant tenant, String externalId) {
        this.tenant = tenant;
        this.externalId = externalId;
    }
}