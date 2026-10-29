package com.notifyhub.template;

import com.notifyhub.common.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    Optional<Template> findByTenantIdAndNameAndChannelAndActiveTrue(Long tenantId, String name, Channel channel);
}