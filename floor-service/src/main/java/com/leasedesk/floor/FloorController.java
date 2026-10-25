package com.leasedesk.floor;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.Channels;
import com.leasedesk.common.FloorOpened;
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
public class FloorController {

    private static final Set<String> STATUSES = Set.of("OPEN", "CLOSED");

    private final FloorRepository floors;
    private final BuildingViewRepository buildings;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public FloorController(FloorRepository floors, BuildingViewRepository buildings, OutboxRepository outbox, ObjectMapper mapper) {
        this.floors = floors;
        this.buildings = buildings;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/floors")
    @Transactional
    public ResponseEntity<FloorEntity> create(@RequestBody CreateFloor request) {
        if (request == null || blank(request.floorCode()) || blank(request.buildingId()) || blank(request.status())
                || !STATUSES.contains(request.status().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "floor fields are required");
        }
        if (!buildings.existsById(request.buildingId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "building not found");
        }
        String code = request.floorCode().trim();
        if (floors.existsByFloorCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "floor already exists");
        }
        FloorEntity row = new FloorEntity();
        row.setId(UUID.randomUUID().toString());
        row.setBuildingId(request.buildingId().trim());
        row.setFloorCode(code);
        row.setStatus(request.status().trim());
        floors.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.FLOORS, row.getId(),
                new FloorOpened(row.getId(), row.getBuildingId(), row.getFloorCode(), row.getStatus()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/floors/{id}")
    public FloorEntity get(@PathVariable String id) {
        return floors.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "floor not found"));
    }

    @GetMapping("/directory/buildings/{id}")
    public BuildingView building(@PathVariable String id) {
        return buildings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "building not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateFloor(String floorCode, String buildingId, String status) {
    }
}
