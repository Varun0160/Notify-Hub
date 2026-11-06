package com.notifyhub.template;

import com.notifyhub.common.enums.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    Optional<Template> findByTenantIdAndNameAndChannelAndActiveTrue(
            Long tenantId, String name, Channel channel);

    Optional<Template> findByTenantIdAndNameAndChannelAndVersionNo(
            Long tenantId, String name, Channel channel, int versionNo);

    List<Template> findByTenantIdAndNameAndChannelOrderByVersionNoDesc(
            Long tenantId, String name, Channel channel);

    List<Template> findByTenantIdAndActiveTrueOrderByNameAscChannelAsc(Long tenantId);

    @Query("select coalesce(max(t.versionNo), 0) from Template t "
            + "where t.tenant.id = :tenantId and t.name = :name and t.channel = :channel")
    int findMaxVersion(@Param("tenantId") Long tenantId,
                       @Param("name") String name,
                       @Param("channel") Channel channel);

    @Modifying
    @Query("update Template t set t.active = false "
            + "where t.tenant.id = :tenantId and t.name = :name "
            + "and t.channel = :channel and t.active = true")
    int deactivateAll(@Param("tenantId") Long tenantId,
                      @Param("name") String name,
                      @Param("channel") Channel channel);
}