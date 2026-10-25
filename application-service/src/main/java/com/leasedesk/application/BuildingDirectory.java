package com.leasedesk.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "building_directory")
public class BuildingDirectory {
    @Id
    private String id;
    @Column(name = "building_code")
    private String buildingCode;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuildingCode() { return buildingCode; }
    public void setBuildingCode(String buildingCode) { this.buildingCode = buildingCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
