package com.notifyhub.recipient;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.recipient.dto.CreateRecipientRequest;
import com.notifyhub.recipient.dto.PreferenceResponse;
import com.notifyhub.recipient.dto.RecipientResponse;
import com.notifyhub.recipient.dto.UpdatePreferenceRequest;
import com.notifyhub.recipient.dto.UpdateRecipientRequest;
import com.notifyhub.security.TenantPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipients")
@RequiredArgsConstructor
public class RecipientController {

    private final RecipientService recipientService;

    @PostMapping
    public ResponseEntity<RecipientResponse> create(@AuthenticationPrincipal TenantPrincipal principal,
                                                    @Valid @RequestBody CreateRecipientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recipientService.create(principal.tenantId(), request));
    }

    @GetMapping("/{id}")
    public RecipientResponse get(@AuthenticationPrincipal TenantPrincipal principal,
                                 @PathVariable("id") String id) {
        return recipientService.get(principal.tenantId(), id);
    }

    @PutMapping("/{id}")
    public RecipientResponse update(@AuthenticationPrincipal TenantPrincipal principal,
                                    @PathVariable("id") String id,
                                    @Valid @RequestBody UpdateRecipientRequest request) {
        return recipientService.update(principal.tenantId(), id, request);
    }

    @GetMapping("/{id}/preferences")
    public List<PreferenceResponse> getPreferences(@AuthenticationPrincipal TenantPrincipal principal,
                                                   @PathVariable("id") String id) {
        return recipientService.getPreferences(principal.tenantId(), id);
    }

    @PutMapping("/{id}/preferences/{channel}")
    public PreferenceResponse setPreference(@AuthenticationPrincipal TenantPrincipal principal,
                                            @PathVariable("id") String id,
                                            @PathVariable("channel") Channel channel,
                                            @Valid @RequestBody UpdatePreferenceRequest request) {
        return recipientService.setPreference(principal.tenantId(), id, channel, request.enabled());
    }
}