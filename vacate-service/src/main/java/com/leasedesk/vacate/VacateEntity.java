package com.leasedesk.vacate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vacates")
public class VacateEntity {
    @Id
    private String id;
    @Column(name = "unit_id")
    private String unitId;
    @Column(name = "fee_cents")
    private int feeCents;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public int getFeeCents() { return feeCents; }
    public void setFeeCents(int feeCents) { this.feeCents = feeCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
