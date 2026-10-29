package com.notifyhub.tenant;

import com.notifyhub.common.BaseEntity;
import com.notifyhub.common.enums.ApiKeyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 12)
    private String keyPrefix;

    @Column(nullable = false, length = 64)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    private Instant expiresAt;
    private Instant lastUsedAt;

    public ApiKey(Tenant tenant, String name, String keyPrefix, String keyHash) {
        this.tenant = tenant;
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
    }
}