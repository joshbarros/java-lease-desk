package com.leasedesk.building;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<BuildingEntity, String> {
boolean existsByBuildingCode(String buildingCode);
}
