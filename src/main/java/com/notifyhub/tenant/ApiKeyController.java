package com.notifyhub.tenant;

import com.notifyhub.tenant.dto.ApiKeyCreatedResponse;
import com.notifyhub.tenant.dto.ApiKeyResponse;
import com.notifyhub.tenant.dto.CreateApiKeyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> create(@PathVariable("tenantId") String tenantId,
                                                        @Valid @RequestBody CreateApiKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.create(tenantId, request));
    }

    @GetMapping
    public List<ApiKeyResponse> list(@PathVariable("tenantId") String tenantId) {
        return apiKeyService.list(tenantId);
    }

    @DeleteMapping("/{keyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable("tenantId") String tenantId,
                       @PathVariable("keyId") String keyId) {
        apiKeyService.revoke(tenantId, keyId);
    }
}