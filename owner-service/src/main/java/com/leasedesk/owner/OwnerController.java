package com.leasedesk.owner;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.OwnerOpened;
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
public class OwnerController {

    private final OwnerRepository owners;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public OwnerController(OwnerRepository owners, OutboxRepository outbox, ObjectMapper mapper) {
        this.owners = owners;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/owners")
    @Transactional
    public ResponseEntity<OwnerEntity> create(@RequestBody CreateOwner request) {
        if (request == null || blank(request.ownerCode()) || blank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "owner fields are required");
        }
        String code = request.ownerCode().trim();
        if (owners.existsByOwnerCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "owner already exists");
        }
        OwnerEntity row = new OwnerEntity();
        row.setId(UUID.randomUUID().toString());
        row.setOwnerCode(code);
        row.setName(request.name().trim());
        owners.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.OWNERS, row.getId(),
                new OwnerOpened(row.getId(), row.getOwnerCode(), row.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/owners/{id}")
    public OwnerEntity get(@PathVariable String id) {
        return owners.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateOwner(String ownerCode, String name) {
    }
}
