package com.leasedesk.unit;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.UnitOpened;
import com.leasedesk.common.Channels;
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
public class UnitController {

    private final UnitRepository units;
    private final HoldRepository holds;
    private final OwnerViewRepository owners;
    private final BuildingViewRepository buildings;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public UnitController(UnitRepository units, HoldRepository holds, OwnerViewRepository owners,
            BuildingViewRepository buildings, OutboxRepository outbox, ObjectMapper mapper) {
        this.units = units;
        this.holds = holds;
        this.owners = owners;
        this.buildings = buildings;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/units")
    @Transactional
    public ResponseEntity<UnitEntity> create(@RequestBody CreateUnit request) {
        if (request == null || blank(request.unitCode()) || blank(request.ownerId()) || blank(request.buildingId())
                || request.slots() == null || request.slots() <= 0
                || request.rentCents() == null || request.rentCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unit fields are required");
        }
        if (!owners.existsById(request.ownerId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "owner not found");
        }
        if (!buildings.existsById(request.buildingId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "building not found");
        }
        String code = request.unitCode().trim();
        if (units.existsByUnitCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "unit already exists");
        }
        UnitEntity row = new UnitEntity();
        row.setId(UUID.randomUUID().toString());
        row.setOwnerId(request.ownerId().trim());
        row.setBuildingId(request.buildingId().trim());
        row.setUnitCode(code);
        row.setSlots(request.slots());
        row.setRentCents(request.rentCents());
        row.setOpenSlots(request.slots());
        row.setStatus("OPEN");
        units.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.UNITS, row.getId(),
                new UnitOpened(row.getId(), row.getOwnerId(), row.getBuildingId(), row.getUnitCode(),
                        row.getSlots(), row.getRentCents()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/units/{id}")
    public UnitEntity get(@PathVariable String id) {
        return units.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unit not found"));
    }

    @GetMapping("/holds/{applicationId}")
    public HoldEntity hold(@PathVariable String applicationId) {
        return holds.findById(applicationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hold not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateUnit(String unitCode, String ownerId, String buildingId, Integer slots, Integer rentCents) {
    }
}
