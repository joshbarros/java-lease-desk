package com.leasedesk.application;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leasedesk.common.CancelCommand;
import com.leasedesk.common.Channels;
import com.leasedesk.common.SignCommand;
import com.leasedesk.common.RateCommand;
import com.leasedesk.common.ApplicationFact;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
public class ApplicationController {

    private static final Set<String> BREAKLABLE = Set.of("READY", "SIGNED");

    private final ApplicationRepository applications;
    private final ApplicationActionRepository actions;
    private final OwnerDirectoryRepository owners;
    private final BuildingDirectoryRepository buildings;
    private final TenantDirectoryRepository tenants;
    private final FloorDirectoryRepository floors;
    private final UnitDirectoryRepository units;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ApplicationController(ApplicationRepository applications, ApplicationActionRepository actions, OwnerDirectoryRepository owners,
            BuildingDirectoryRepository buildings, TenantDirectoryRepository tenants, FloorDirectoryRepository floors,
            UnitDirectoryRepository units, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.applications = applications;
        this.actions = actions;
        this.owners = owners;
        this.buildings = buildings;
        this.tenants = tenants;
        this.floors = floors;
        this.units = units;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @PostMapping("/applications")
    @Transactional
    public ResponseEntity<ApplicationEntity> create(@RequestBody CreateApplication request) {
        if (request == null || blank(request.requestId()) || blank(request.ownerId()) || blank(request.buildingId())
                || blank(request.tenantId()) || blank(request.floorId()) || blank(request.unitId())
                || request.months() == null || request.months() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "application fields are required");
        }
        ApplicationEntity existing = applications.findByRequestId(request.requestId().trim()).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(existing);
        }
        OwnerDirectory owner = owners.findById(request.ownerId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner not found"));
        BuildingDirectory building = buildings.findById(request.buildingId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "building not found"));
        TenantDirectory tenant = tenants.findById(request.tenantId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tenant not found"));
        FloorDirectory floor = floors.findById(request.floorId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "floor not found"));
        UnitDirectory unit = units.findById(request.unitId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unit not found"));
        if (!"OPEN".equals(building.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "building is closed");
        }
        if (!"OPEN".equals(floor.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "floor is closed");
        }
        if (!building.getId().equals(floor.getBuildingId()) || !building.getId().equals(unit.getBuildingId())
                || !owner.getId().equals(unit.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "application does not belong to that building and owner");
        }
        ApplicationEntity row = new ApplicationEntity();
        row.setId(UUID.randomUUID().toString());
        row.setRequestId(request.requestId().trim());
        row.setOwnerId(owner.getId());
        row.setBuildingId(building.getId());
        row.setTenantId(tenant.getId());
        row.setFloorId(floor.getId());
        row.setUnitId(unit.getId());
        row.setTenantCode(tenant.getTenantCode());
        row.setMonths(request.months());
        row.setRentCents(unit.getRentCents());
        row.setFeeCents(0);
        row.setStatus("QUOTING");
        row.setReason("");
        applications.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.APPLICATIONS, row.getId(), new ApplicationFact(row.getId(), row.getStatus(), ""));
        OutboxWriter.rabbit(outbox, mapper, Channels.RATE, row.getId(),
                new RateCommand(row.getId(), row.getMonths(), row.getRentCents()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/applications/{id}")
    public ApplicationEntity get(@PathVariable String id) {
        return applications.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "application not found"));
    }

    @PostMapping("/applications/{id}/signatures")
    @Transactional
    public ResponseEntity<ApplicationEntity> sign(@PathVariable String id, @RequestBody ActionRequest request) {
        ApplicationEntity prior = replay(request);
        if (prior != null) {
            return ResponseEntity.ok(prior);
        }
        ApplicationEntity row = lock(id);
        if (!"READY".equals(row.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "application is closed");
        }
        remember(request.requestId().trim(), row.getId(), "SIGN");
        OutboxWriter.rabbit(outbox, mapper, Channels.SIGN, row.getId(), new SignCommand(row.getId()));
        return ResponseEntity.accepted().body(row);
    }

    @PostMapping("/applications/{id}/breaks")
    @Transactional
    public ResponseEntity<ApplicationEntity> cancel(@PathVariable String id, @RequestBody ActionRequest request) {
        ApplicationEntity prior = replay(request);
        if (prior != null) {
            return ResponseEntity.ok(prior);
        }
        ApplicationEntity row = lock(id);
        if (!BREAKLABLE.contains(row.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "application is closed");
        }
        remember(request.requestId().trim(), row.getId(), "BREAK");
        OutboxWriter.rabbit(outbox, mapper, Channels.BREAK, row.getId(),
                new CancelCommand(row.getId(), row.getUnitId(), row.getFeeCents()));
        return ResponseEntity.accepted().body(row);
    }

    @GetMapping("/directory/owners/{id}")
    public OwnerDirectory owner(@PathVariable String id) {
        return owners.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "owner not found"));
    }

    @GetMapping("/directory/buildings/{id}")
    public BuildingDirectory building(@PathVariable String id) {
        return buildings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "building not found"));
    }

    @GetMapping("/directory/tenants/{id}")
    public TenantDirectory tenant(@PathVariable String id) {
        return tenants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "tenant not found"));
    }

    @GetMapping("/directory/floors/{id}")
    public FloorDirectory floor(@PathVariable String id) {
        return floors.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "floor not found"));
    }

    @GetMapping("/directory/units/{id}")
    public UnitDirectory unit(@PathVariable String id) {
        return units.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unit not found"));
    }

    private ApplicationEntity replay(ActionRequest request) {
        if (request == null || blank(request.requestId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request id is required");
        }
        ApplicationAction prior = actions.findById(request.requestId().trim()).orElse(null);
        if (prior == null) {
            return null;
        }
        return applications.findById(prior.getApplicationId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "application not found"));
    }

    private void remember(String requestId, String applicationId, String kind) {
        ApplicationAction action = new ApplicationAction();
        action.setId(requestId);
        action.setApplicationId(applicationId);
        action.setKind(kind);
        actions.save(action);
    }

    private ApplicationEntity lock(String id) {
        ApplicationEntity row = em.find(ApplicationEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "application not found");
        }
        return row;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateApplication(String requestId, String ownerId, String buildingId, String tenantId, String floorId,
            String unitId, Integer months) {
    }

    public record ActionRequest(String requestId) {
    }
}
