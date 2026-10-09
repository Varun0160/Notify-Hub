package com.hookrelay.hookrelay.tenant;

import com.hookrelay.hookrelay.security.CurrentTenant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/me")
    public TenantResponse me(@AuthenticationPrincipal Jwt jwt) {
        return tenantService.getById(CurrentTenant.id(jwt));
    }
}