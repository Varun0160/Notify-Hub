package com.notifyhub.template;

import com.notifyhub.common.BaseEntity;
import com.notifyhub.common.enums.Channel;
import com.notifyhub.tenant.Tenant;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Template extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(length = 255)
    private String subject;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean active = true;
}