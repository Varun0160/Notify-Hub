package com.notifyhub.template.dto;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.template.Template;
import com.notifyhub.template.TemplateRenderer;

import java.time.Instant;
import java.util.List;

public record TemplateResponse(
        String name,
        Channel channel,
        int version,
        String subject,
        String body,
        boolean active,
        List<String> variables,
        Instant createdAt
) {
    public static TemplateResponse from(Template t) {
        return new TemplateResponse(
                t.getName(), t.getChannel(), t.getVersionNo(), t.getSubject(), t.getBody(),
                t.isActive(),
                List.copyOf(TemplateRenderer.extractVariables(t.getSubject(), t.getBody())),
                t.getCreatedAt());
    }
}