package com.notifyhub.tenant;

import com.notifyhub.tenant.dto.CreateTenantRequest;
import com.notifyhub.tenant.dto.TenantResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@Tag(name = "Admin")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(request));
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable("id") String id) {
        return tenantService.getByPublicId(id);
    }
}