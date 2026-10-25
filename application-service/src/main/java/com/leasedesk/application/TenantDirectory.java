package com.leasedesk.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tenant_directory")
public class TenantDirectory {
    @Id
    private String id;
    @Column(name = "tenant_code")
    private String tenantCode;
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantCode() { return tenantCode; }
    public void setTenantCode(String tenantCode) { this.tenantCode = tenantCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
