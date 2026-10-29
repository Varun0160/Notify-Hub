package com.notifyhub.tenant;

import com.notifyhub.common.BaseEntity;
import com.notifyhub.common.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tenant extends BaseEntity {

    @Column(nullable = false, updatable = false, length = 36)
    private String publicId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantStatus status = TenantStatus.ACTIVE;

    public Tenant(String name) {
        this.name = name;
    }
}