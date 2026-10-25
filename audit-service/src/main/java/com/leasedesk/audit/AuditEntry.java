package com.leasedesk.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_entries")
public class AuditEntry {
    @Id
    private String id;
    @Column(name = "application_id")
    private String applicationId;
    private String kind;
    private String status;
    @Column(name = "recorded_at")
    private java.time.Instant recordedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.time.Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(java.time.Instant recordedAt) { this.recordedAt = recordedAt; }
}
