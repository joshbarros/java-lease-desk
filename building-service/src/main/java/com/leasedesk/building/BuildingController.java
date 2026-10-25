package com.leasedesk.building;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.BuildingOpened;
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
public class BuildingController {

    private static final Set<String> STATUSES = Set.of("OPEN", "CLOSED");

    private final BuildingRepository buildings;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public BuildingController(BuildingRepository buildings, OutboxRepository outbox, ObjectMapper mapper) {
        this.buildings = buildings;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/buildings")
    @Transactional
    public ResponseEntity<BuildingEntity> create(@RequestBody CreateBuilding request) {
        if (request == null || blank(request.buildingCode()) || blank(request.status())
                || !STATUSES.contains(request.status().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "building fields are required");
        }
        String code = request.buildingCode().trim();
        if (buildings.existsByBuildingCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "building already exists");
        }
        BuildingEntity row = new BuildingEntity();
        row.setId(UUID.randomUUID().toString());
        row.setBuildingCode(code);
        row.setStatus(request.status().trim());
        buildings.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.BUILDINGS, row.getId(),
                new BuildingOpened(row.getId(), row.getBuildingCode(), row.getStatus()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/buildings/{id}")
    public BuildingEntity get(@PathVariable String id) {
        return buildings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "building not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateBuilding(String buildingCode, String status) {
    }
}
