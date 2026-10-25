package com.leasedesk.tenant;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.TenantOpened;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TenantController {

    private final TenantRepository tenants;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public TenantController(TenantRepository tenants, OutboxRepository outbox, ObjectMapper mapper) {
        this.tenants = tenants;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/tenants")
    @Transactional
    public ResponseEntity<TenantEntity> create(@RequestBody CreateTenant request) {
        if (request == null || blank(request.tenantCode()) || blank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenant fields are required");
        }
        String code = request.tenantCode().trim();
        if (tenants.existsByTenantCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "tenant already exists");
        }
        TenantEntity row = new TenantEntity();
        row.setId(UUID.randomUUID().toString());
        row.setTenantCode(code);
        row.setName(request.name().trim());
        tenants.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.TENANTS, row.getId(),
                new TenantOpened(row.getId(), row.getTenantCode(), row.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/tenants/{id}")
    public TenantEntity get(@PathVariable String id) {
        return tenants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tenant not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateTenant(String tenantCode, String name) {
    }
}
