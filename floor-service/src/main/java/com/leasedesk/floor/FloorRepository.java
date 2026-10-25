package com.leasedesk.floor;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<FloorEntity, String> {
boolean existsByFloorCode(String floorCode);
}
