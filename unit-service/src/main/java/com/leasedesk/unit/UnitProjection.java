package com.leasedesk.unit;

import com.leasedesk.common.Channels;
import com.leasedesk.common.OwnerOpened;
import com.leasedesk.common.BuildingOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UnitProjection {

    private final OwnerViewRepository owners;
    private final BuildingViewRepository buildings;

    public UnitProjection(OwnerViewRepository owners, BuildingViewRepository buildings) {
        this.owners = owners;
        this.buildings = buildings;
    }

    @KafkaListener(topics = Channels.OWNERS, groupId = "unit-owners")
    @Transactional
    public void onOwner(OwnerOpened event) {
        if (owners.existsById(event.ownerId())) {
            return;
        }
        OwnerView row = new OwnerView();
        row.setId(event.ownerId());
        row.setOwnerCode(event.ownerCode());
        row.setName(event.name());
        owners.save(row);
    }

    @KafkaListener(topics = Channels.BUILDINGS, groupId = "unit-buildings")
    @Transactional
    public void onBuilding(BuildingOpened event) {
        if (buildings.existsById(event.buildingId())) {
            return;
        }
        BuildingView row = new BuildingView();
        row.setId(event.buildingId());
        row.setBuildingCode(event.buildingCode());
        row.setStatus(event.status());
        buildings.save(row);
    }
}
