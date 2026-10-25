package com.leasedesk.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_actions")
public class ApplicationAction {
    @Id
    private String id;
    @Column(name = "application_id")
    private String applicationId;
    private String kind;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
}
