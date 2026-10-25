package com.leasedesk.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "unit_directory")
public class UnitDirectory {
    @Id
    private String id;
    @Column(name = "owner_id")
    private String ownerId;
    @Column(name = "building_id")
    private String buildingId;
    @Column(name = "unit_code")
    private String unitCode;
    private int slots;
    @Column(name = "rent_cents")
    private int rentCents;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
    public int getSlots() { return slots; }
    public void setSlots(int slots) { this.slots = slots; }
    public int getRentCents() { return rentCents; }
    public void setRentCents(int rentCents) { this.rentCents = rentCents; }
}
