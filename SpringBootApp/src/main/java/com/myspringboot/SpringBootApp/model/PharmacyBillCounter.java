package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;

/**
 * Atomic per-pharmacy bill sequence counter.
 *
 * One row per pharmacy. The bill_seq column is incremented inside a
 * SELECT ... FOR UPDATE to guarantee uniqueness even under concurrent load.
 *
 * Table: pharmacy_bill_counters
 */
@Entity
@Table(name = "pharmacy_bill_counters")
public class PharmacyBillCounter {

    @Id
    @Column(name = "pharmacy_id")
    private Long pharmacyId;

    @Column(name = "bill_seq", nullable = false)
    private long billSeq = 0L;

    public PharmacyBillCounter() {}

    public PharmacyBillCounter(Long pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public Long getPharmacyId()              { return pharmacyId; }
    public void setPharmacyId(Long id)       { this.pharmacyId = id; }

    public long getBillSeq()                 { return billSeq; }
    public void setBillSeq(long billSeq)     { this.billSeq = billSeq; }

    /** Increment and return the next sequence number. */
    public long nextSeq() {
        this.billSeq += 1;
        return this.billSeq;
    }
}
