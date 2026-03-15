package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit log entry — one row per significant user action.
 * Table: audit_logs
 */
@Entity
@Table(name = "audit_logs",
       indexes = { @Index(name = "idx_audit_pharmacy", columnList = "pharmacy_id"),
                   @Index(name = "idx_audit_user",     columnList = "user_id"),
                   @Index(name = "idx_audit_ts",       columnList = "timestamp") })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The ID of the user who performed the action (null = system). */
    @Column(name = "user_id")
    private Long userId;

    /** The username/email at the time of the action (denormalised for read speed). */
    @Column(name = "username", length = 100)
    private String username;

    /** Scoping key — which pharmacy this action belongs to. */
    @Column(name = "pharmacy_id")
    private Long pharmacyId;

    /** Human-readable action, e.g. MEDICINE_CREATED, BILL_CANCELLED. */
    @Column(nullable = false, length = 60)
    private String action;

    /** Entity type, e.g. "Medicine", "Billing". */
    @Column(name = "entity_type", length = 40)
    private String entityType;

    /** PK of the entity that was affected. */
    @Column(name = "entity_id")
    private Long entityId;

    /** When this action was performed. */
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Free-form detail text — up to 1000 chars. */
    @Column(length = 1000)
    private String details;

    public AuditLog() {}

    // ── Convenience factory ───────────────────────────────────────────────

    public static AuditLog of(User user, Long pharmacyId, String action, String entityType,
                               Long entityId, String details) {
        AuditLog log = new AuditLog();
        if (user != null) {
            log.userId     = user.getId();
            log.username   = user.getEmail();
        }
        log.pharmacyId = pharmacyId;          // resolved by caller — never lazy-loaded here
        log.action     = action;
        log.entityType = entityType;
        log.entityId   = entityId;
        log.details    = details;
        return log;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public Long getId()                       { return id; }
    public Long getUserId()                   { return userId; }
    public void setUserId(Long userId)        { this.userId = userId; }
    public String getUsername()               { return username; }
    public void setUsername(String username)  { this.username = username; }
    public Long getPharmacyId()               { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId){ this.pharmacyId = pharmacyId; }
    public String getAction()                 { return action; }
    public void setAction(String action)      { this.action = action; }
    public String getEntityType()             { return entityType; }
    public void setEntityType(String t)       { this.entityType = t; }
    public Long getEntityId()                 { return entityId; }
    public void setEntityId(Long entityId)    { this.entityId = entityId; }
    public LocalDateTime getTimestamp()       { return timestamp; }
    public void setTimestamp(LocalDateTime t) { this.timestamp = t; }
    public String getDetails()                { return details; }
    public void setDetails(String details)    { this.details = details; }
}
