package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.AuditLog;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Centralized service for writing audit log entries.
 * All writes are @Async so they never delay the main transaction.
 *
 * pharmacyId is resolved by the caller BEFORE dispatching here,
 * to avoid LazyInitializationException when user.getPharmacy() is
 * accessed inside an async thread where the JPA session is closed.
 */
@Service
public class AuditLogService {

    @Autowired private AuditLogRepository auditLogRepository;

    /**
     * Record an audit event.
     *
     * @param user        the authenticated user performing the action (may be null)
     * @param pharmacyId  explicitly resolved pharmacy ID — do NOT derive from user inside async
     * @param action      a constant like "MEDICINE_CREATED", "BILL_CANCELLED"
     * @param entityType  human-readable entity name, e.g. "Medicine", "Billing"
     * @param entityId    primary key of the affected entity
     * @param details     free-form description / diff text
     */
    @Async
    public void log(User user, Long pharmacyId,
                    String action, String entityType, Long entityId, String details) {
        try {
            auditLogRepository.save(
                    AuditLog.of(user, pharmacyId, action, entityType, entityId, details));
        } catch (Exception ignored) {
            // Audit failures must never propagate to the caller
        }
    }
}
