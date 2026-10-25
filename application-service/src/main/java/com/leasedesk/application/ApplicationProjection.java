package com.leasedesk.application;

import com.leasedesk.common.UnitOpened;
import com.leasedesk.common.Channels;
import com.leasedesk.common.FloorOpened;
import com.leasedesk.common.TenantOpened;
import com.leasedesk.common.OwnerOpened;
import com.leasedesk.common.BuildingOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ApplicationProjection {

    private final OwnerDirectoryRepository owners;
    private final BuildingDirectoryRepository buildings;
    private final TenantDirectoryRepository tenants;
    private final FloorDirectoryRepository floors;
    private final UnitDirectoryRepository units;

    public ApplicationProjection(OwnerDirectoryRepository owners, BuildingDirectoryRepository buildings,
            TenantDirectoryRepository tenants, FloorDirectoryRepository floors, UnitDirectoryRepository units) {
        this.owners = owners;
        this.buildings = buildings;
        this.tenants = tenants;
        this.floors = floors;
        this.units = units;
    }

    @KafkaListener(topics = Channels.OWNERS, groupId = "application-owners")
    @Transactional
    public void onOwner(OwnerOpened event) {
        if (owners.existsById(event.ownerId())) {
            return;
        }
        OwnerDirectory row = new OwnerDirectory();
        row.setId(event.ownerId());
        row.setOwnerCode(event.ownerCode());
        row.setName(event.name());
        owners.save(row);
    }

    @KafkaListener(topics = Channels.BUILDINGS, groupId = "application-buildings")
    @Transactional
    public void onBuilding(BuildingOpened event) {
        if (buildings.existsById(event.buildingId())) {
            return;
        }
        BuildingDirectory row = new BuildingDirectory();
        row.setId(event.buildingId());
        row.setBuildingCode(event.buildingCode());
        row.setStatus(event.status());
        buildings.save(row);
    }

    @KafkaListener(topics = Channels.TENANTS, groupId = "application-tenants")
    @Transactional
    public void onTenant(TenantOpened event) {
        if (tenants.existsById(event.tenantId())) {
            return;
        }
        TenantDirectory row = new TenantDirectory();
        row.setId(event.tenantId());
        row.setTenantCode(event.tenantCode());
        row.setName(event.name());
        tenants.save(row);
    }

    @KafkaListener(topics = Channels.FLOORS, groupId = "application-floors")
    @Transactional
    public void onFloor(FloorOpened event) {
        if (floors.existsById(event.floorId())) {
            return;
        }
        FloorDirectory row = new FloorDirectory();
        row.setId(event.floorId());
        row.setBuildingId(event.buildingId());
        row.setFloorCode(event.floorCode());
        row.setStatus(event.status());
        floors.save(row);
    }

    @KafkaListener(topics = Channels.UNITS, groupId = "application-units")
    @Transactional
    public void onUnit(UnitOpened event) {
        if (units.existsById(event.unitId())) {
            return;
        }
        UnitDirectory row = new UnitDirectory();
        row.setId(event.unitId());
        row.setOwnerId(event.ownerId());
        row.setBuildingId(event.buildingId());
        row.setUnitCode(event.unitCode());
        row.setSlots(event.slots());
        row.setRentCents(event.rentCents());
        units.save(row);
    }
}
