package com.leasedesk.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "floor_directory")
public class FloorDirectory {
    @Id
    private String id;
    @Column(name = "building_id")
    private String buildingId;
    @Column(name = "floor_code")
    private String floorCode;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public String getFloorCode() { return floorCode; }
    public void setFloorCode(String floorCode) { this.floorCode = floorCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
