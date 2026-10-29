package com.notifyhub.recipient;

import com.notifyhub.common.BaseEntity;
import com.notifyhub.common.enums.Channel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recipient_preferences")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipientPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Column(nullable = false)
    private boolean enabled = true;

    public RecipientPreference(Recipient recipient, Channel channel, boolean enabled) {
        this.recipient = recipient;
        this.channel = channel;
        this.enabled = enabled;
    }
}