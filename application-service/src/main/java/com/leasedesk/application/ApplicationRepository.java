package com.leasedesk.application;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, String> {
java.util.Optional<ApplicationEntity> findByRequestId(String requestId);
}
