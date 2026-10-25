package com.leasedesk.unit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldRepository extends JpaRepository<HoldEntity, String> {
@org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "INSERT INTO holds (id, unit_id, fee_cents, status) VALUES (:id, :unitId, :fee, 'HELD')", nativeQuery = true)
    int insertHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("unitId") String unitId, @org.springframework.data.repository.query.Param("fee") int fee);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE holds SET status = 'RELEASED' WHERE id = :id AND unit_id = :unitId AND status = 'HELD' AND fee_cents = :fee", nativeQuery = true)
    int vacateHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("unitId") String unitId, @org.springframework.data.repository.query.Param("fee") int fee);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE holds SET status = 'KEPT' WHERE id = :id AND unit_id = :unitId AND status = 'HELD' AND fee_cents = :fee", nativeQuery = true)
    int keepHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("unitId") String unitId, @org.springframework.data.repository.query.Param("fee") int fee);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE holds SET status = 'RETURNED' WHERE id = :id AND unit_id = :unitId AND status = 'KEPT' AND fee_cents = :fee", nativeQuery = true)
    int restoreHold(@org.springframework.data.repository.query.Param("id") String id, @org.springframework.data.repository.query.Param("unitId") String unitId, @org.springframework.data.repository.query.Param("fee") int fee);
}
