package com.myspringboot.SpringBootApp.repo;

import com.myspringboot.SpringBootApp.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPharmacyIdOrderByTimestampDesc(Long pharmacyId);
    List<AuditLog> findByPharmacyIdAndEntityTypeOrderByTimestampDesc(Long pharmacyId, String entityType);
}
