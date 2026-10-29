package com.notifyhub.recipient;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import com.notifyhub.common.enums.Channel;

public interface RecipientPreferenceRepository extends JpaRepository<RecipientPreference, Long> {
    List<RecipientPreference> findByRecipientId(Long recipientId);
    Optional<RecipientPreference> findByRecipientIdAndChannel(Long recipientId, Channel channel);
}