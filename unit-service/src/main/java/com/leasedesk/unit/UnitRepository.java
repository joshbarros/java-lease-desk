package com.leasedesk.unit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitRepository extends JpaRepository<UnitEntity, String> {
boolean existsByUnitCode(String unitCode);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE units SET open_slots = open_slots - 1 WHERE id = :id AND open_slots >= 1", nativeQuery = true)
    int reserve(@org.springframework.data.repository.query.Param("id") String id);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE units SET open_slots = open_slots + 1 WHERE id = :id", nativeQuery = true)
    int credit(@org.springframework.data.repository.query.Param("id") String id);
}
