package com.leasedesk.floor;

import com.leasedesk.common.Channels;
import com.leasedesk.common.BuildingOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FloorProjection {

    private final BuildingViewRepository buildings;

    public FloorProjection(BuildingViewRepository buildings) {
        this.buildings = buildings;
    }

    @KafkaListener(topics = Channels.BUILDINGS, groupId = "floor-buildings")
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
