package com.leasedesk.unit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "owner_views")
public class OwnerView {
    @Id
    private String id;
    @Column(name = "owner_code")
    private String ownerCode;
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerCode() { return ownerCode; }
    public void setOwnerCode(String ownerCode) { this.ownerCode = ownerCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
