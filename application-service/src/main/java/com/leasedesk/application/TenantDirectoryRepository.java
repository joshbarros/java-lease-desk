package com.leasedesk.application;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDirectoryRepository extends JpaRepository<TenantDirectory, String> {

}
