package com.hookrelay.hookrelay.endpoint;

import com.hookrelay.hookrelay.security.CurrentTenant;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/endpoints")
public class EndpointController {

    private final EndpointService endpointService;

    public EndpointController(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @PostMapping
    public ResponseEntity<EndpointResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Valid @RequestBody CreateEndpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(endpointService.create(CurrentTenant.id(jwt), request));
    }

    @GetMapping
    public Page<EndpointResponse> list(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("id").descending());
        return endpointService.list(CurrentTenant.id(jwt), pageable);
    }

    @GetMapping("/{endpointId}")
    public EndpointResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long endpointId) {
        return endpointService.get(CurrentTenant.id(jwt), endpointId);
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long endpointId) {
        endpointService.delete(CurrentTenant.id(jwt), endpointId);
        return ResponseEntity.noContent().build();
    }
}