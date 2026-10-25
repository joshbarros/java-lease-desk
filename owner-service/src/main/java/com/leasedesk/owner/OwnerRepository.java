package com.leasedesk.owner;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerRepository extends JpaRepository<OwnerEntity, String> {
boolean existsByOwnerCode(String ownerCode);
}
