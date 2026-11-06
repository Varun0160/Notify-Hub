package com.notifyhub.template;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.common.exception.ResourceNotFoundException;
import com.notifyhub.template.dto.CreateTemplateRequest;
import com.notifyhub.template.dto.PreviewRequest;
import com.notifyhub.template.dto.PreviewResponse;
import com.notifyhub.template.dto.TemplateResponse;
import com.notifyhub.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final int MAX_PREVIEW_VARIABLES = 50;
    private static final int MAX_VARIABLE_LENGTH = 2000;

    private final TemplateRepository templateRepository;
    private final TenantRepository tenantRepository;

    /** Creates version 1, or version N+1 if the template already exists. */
    @Transactional
    public TemplateResponse createVersion(Long tenantId, CreateTemplateRequest request) {
        String name = request.name().trim();
        Channel channel = request.channel();
        String subject = normalizeSubject(channel, request.subject());

        TemplateRenderer.validateSyntax(subject);
        TemplateRenderer.validateSyntax(request.body());

        int next = templateRepository.findMaxVersion(tenantId, name, channel) + 1;
        templateRepository.deactivateAll(tenantId, name, channel);

        Template template = new Template(tenantRepository.getReferenceById(tenantId),
                name, channel, next, subject, request.body());
        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listActive(Long tenantId) {
        return templateRepository.findByTenantIdAndActiveTrueOrderByNameAscChannelAsc(tenantId)
                .stream().map(TemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getActive(Long tenantId, String name, Channel channel) {
        return TemplateResponse.from(findActive(tenantId, name, channel));
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listVersions(Long tenantId, String name, Channel channel) {
        List<Template> versions = templateRepository
                .findByTenantIdAndNameAndChannelOrderByVersionNoDesc(tenantId, name, channel);
        if (versions.isEmpty()) {
            throw notFound(name, channel);
        }
        return versions.stream().map(TemplateResponse::from).toList();
    }

    @Transactional
    public TemplateResponse activate(Long tenantId, String name, Channel channel, int version) {
        Template target = templateRepository
                .findByTenantIdAndNameAndChannelAndVersionNo(tenantId, name, channel, version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template version not found: " + name + " v" + version + " (" + channel + ")"));

        if (target.isActive()) {
            return TemplateResponse.from(target);        // already active: nothing to do
        }
        templateRepository.deactivateAll(tenantId, name, channel);
        target.setActive(true);                          // dirty checking writes it at commit
        return TemplateResponse.from(target);
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(Long tenantId, String name, Channel channel, PreviewRequest request) {
        Template template = (request.version() == null)
                ? findActive(tenantId, name, channel)
                : templateRepository
                .findByTenantIdAndNameAndChannelAndVersionNo(tenantId, name, channel, request.version())
                .orElseThrow(() -> notFound(name, channel));

        Map<String, String> variables = request.variables();
        checkVariableLimits(variables);

        TemplateRenderer.RenderedMessage rendered = TemplateRenderer.render(
                channel, template.getSubject(), template.getBody(), variables);
        return new PreviewResponse(template.getVersionNo(), rendered.subject(), rendered.body());
    }

    private Template findActive(Long tenantId, String name, Channel channel) {
        return templateRepository
                .findByTenantIdAndNameAndChannelAndActiveTrue(tenantId, name, channel)
                .orElseThrow(() -> notFound(name, channel));
    }

    private ResourceNotFoundException notFound(String name, Channel channel) {
        return new ResourceNotFoundException("Template not found: " + name + " (" + channel + ")");
    }

    private String normalizeSubject(Channel channel, String subject) {
        boolean hasSubject = subject != null && !subject.isBlank();
        if (channel == Channel.EMAIL && !hasSubject) {
            throw new InvalidRequestException("subject is required for EMAIL templates");
        }
        if (channel == Channel.SMS || !hasSubject) {
            return null;                                  // SMS has no subject
        }
        if (subject.contains("\n") || subject.contains("\r")) {
            throw new InvalidRequestException("subject must be a single line");
        }
        return subject.trim();
    }

    private void checkVariableLimits(Map<String, String> variables) {
        if (variables == null) {
            return;
        }
        if (variables.size() > MAX_PREVIEW_VARIABLES) {
            throw new InvalidRequestException("At most " + MAX_PREVIEW_VARIABLES + " variables allowed");
        }
        for (String value : variables.values()) {
            if (value != null && value.length() > MAX_VARIABLE_LENGTH) {
                throw new InvalidRequestException(
                        "Variable values must be at most " + MAX_VARIABLE_LENGTH + " characters");
            }
        }
    }
}