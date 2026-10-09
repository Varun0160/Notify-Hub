package com.hookrelay.hookrelay.endpoint;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/endpoints")
public class EndpointController {

    private final EndpointService endpointService;

    public EndpointController(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @PostMapping
    public ResponseEntity<EndpointResponse> create(@PathVariable Long tenantId,
                                                   @Valid @RequestBody CreateEndpointRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(endpointService.create(tenantId, request));
    }

    @GetMapping
    public Page<EndpointResponse> list(@PathVariable Long tenantId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("id").descending());
        return endpointService.list(tenantId, pageable);
    }

    @GetMapping("/{endpointId}")
    public EndpointResponse get(@PathVariable Long tenantId, @PathVariable Long endpointId) {
        return endpointService.get(tenantId, endpointId);
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<Void> delete(@PathVariable Long tenantId, @PathVariable Long endpointId) {
        endpointService.delete(tenantId, endpointId);
        return ResponseEntity.noContent().build();
    }
}