package com.leasedesk.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {
boolean existsByTenantCode(String tenantCode);
}
