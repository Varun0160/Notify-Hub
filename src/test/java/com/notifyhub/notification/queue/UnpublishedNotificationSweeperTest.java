package com.notifyhub.notification.queue;

import com.notifyhub.common.enums.Channel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnpublishedNotificationSweeperTest {

    private final SweepClaimer claimer = mock(SweepClaimer.class);
    private final NotificationPublisher publisher = mock(NotificationPublisher.class);
    private final UnpublishedNotificationSweeper sweeper =
            new UnpublishedNotificationSweeper(claimer, publisher, 15, 60);

    private SweepClaimer.Claimed claimed(long id) {
        return new SweepClaimer.Claimed(id, "n" + id, Channel.EMAIL);
    }

    @Test
    void publishesEveryClaimedRow() {
        when(claimer.claimBatch(any(), any(), anyInt(), any())).thenReturn(List.of(claimed(1), claimed(2)));
        when(publisher.publish(anyLong(), anyString(), any())).thenReturn(true);

        sweeper.sweep();

        verify(publisher).publish(1L, "n1", Channel.EMAIL);
        verify(publisher).publish(2L, "n2", Channel.EMAIL);
    }

    @Test
    void stopsAtTheFirstFailureBecauseTheBrokerIsProbablyDown() {
        when(claimer.claimBatch(any(), any(), anyInt(), any()))
                .thenReturn(List.of(claimed(1), claimed(2), claimed(3)));
        when(publisher.publish(1L, "n1", Channel.EMAIL)).thenReturn(true);
        when(publisher.publish(2L, "n2", Channel.EMAIL)).thenReturn(false);

        sweeper.sweep();

        verify(publisher, never()).publish(3L, "n3", Channel.EMAIL);
    }

    @Test
    void nothingClaimedMeansNothingPublished() {
        when(claimer.claimBatch(any(), any(), anyInt(), any())).thenReturn(List.of());

        sweeper.sweep();

        verify(publisher, never()).publish(anyLong(), anyString(), any());
    }
}