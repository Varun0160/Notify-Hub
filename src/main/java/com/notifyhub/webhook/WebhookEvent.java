package com.notifyhub.webhook;

import com.notifyhub.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookEvent extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String source;

    @Column(nullable = false, length = 100)
    private String eventId;

    @Column(nullable = false, length = 255)
    private String providerMessageId;

    @Column(nullable = false, length = 30)
    private String eventType;

    @Column(nullable = false, length = 20)
    private String outcome;

    public WebhookEvent(String source, String eventId, String providerMessageId,
                        String eventType, WebhookResult outcome) {
        this.source = source;
        this.eventId = eventId;
        this.providerMessageId = providerMessageId;
        this.eventType = eventType;
        this.outcome = outcome.name();
    }
}