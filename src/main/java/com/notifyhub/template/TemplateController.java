package com.notifyhub.template;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.security.TenantPrincipal;
import com.notifyhub.template.dto.CreateTemplateRequest;
import com.notifyhub.template.dto.PreviewRequest;
import com.notifyhub.template.dto.PreviewResponse;
import com.notifyhub.template.dto.TemplateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@AuthenticationPrincipal TenantPrincipal principal,
                                                   @Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(templateService.createVersion(principal.tenantId(), request));
    }

    @GetMapping
    public List<TemplateResponse> listActive(@AuthenticationPrincipal TenantPrincipal principal) {
        return templateService.listActive(principal.tenantId());
    }

    @GetMapping("/{name}")
    public TemplateResponse getActive(@AuthenticationPrincipal TenantPrincipal principal,
                                      @PathVariable("name") String name,
                                      @RequestParam("channel") Channel channel) {
        return templateService.getActive(principal.tenantId(), name, channel);
    }

    @GetMapping("/{name}/versions")
    public List<TemplateResponse> listVersions(@AuthenticationPrincipal TenantPrincipal principal,
                                               @PathVariable("name") String name,
                                               @RequestParam("channel") Channel channel) {
        return templateService.listVersions(principal.tenantId(), name, channel);
    }

    @PostMapping("/{name}/versions/{version}/activate")
    public TemplateResponse activate(@AuthenticationPrincipal TenantPrincipal principal,
                                     @PathVariable("name") String name,
                                     @PathVariable("version") int version,
                                     @RequestParam("channel") Channel channel) {
        return templateService.activate(principal.tenantId(), name, channel, version);
    }

    @PostMapping("/{name}/preview")
    public PreviewResponse preview(@AuthenticationPrincipal TenantPrincipal principal,
                                   @PathVariable("name") String name,
                                   @RequestParam("channel") Channel channel,
                                   @RequestBody PreviewRequest request) {
        return templateService.preview(principal.tenantId(), name, channel, request);
    }
}