package com.leasedesk.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "applications")
public class ApplicationEntity {
    @Id
    private String id;
    @Column(name = "request_id", unique = true)
    private String requestId;
    @Column(name = "owner_id")
    private String ownerId;
    @Column(name = "building_id")
    private String buildingId;
    @Column(name = "tenant_id")
    private String tenantId;
    @Column(name = "floor_id")
    private String floorId;
    @Column(name = "unit_id")
    private String unitId;
    @Column(name = "tenant_code")
    private String tenantCode;
    private int months;
    @Column(name = "rent_cents")
    private int rentCents;
    @Column(name = "fee_cents")
    private int feeCents;
    private String status;
    private String reason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getBuildingId() { return buildingId; }
    public void setBuildingId(String buildingId) { this.buildingId = buildingId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getFloorId() { return floorId; }
    public void setFloorId(String floorId) { this.floorId = floorId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public int getMonths() { return months; }
    public void setMonths(int months) { this.months = months; }
    public int getRentCents() { return rentCents; }
    public void setRentCents(int rentCents) { this.rentCents = rentCents; }
    public int getFeeCents() { return feeCents; }
    public void setFeeCents(int feeCents) { this.feeCents = feeCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
