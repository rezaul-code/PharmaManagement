package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billings")
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", unique = true)
    private String billNumber;      // e.g. BILL-20240601-0001

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_phone")
    private String patientPhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User createdBy;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillingItem> items = new ArrayList<>();

    // ── Bill-level totals (computed & stored) ──────────────────────

    // Sum of (unitPrice * qty) for all items — before GST
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    // Total GST across all items
    @Column(name = "total_gst", precision = 12, scale = 2)
    private BigDecimal totalGst = BigDecimal.ZERO;

    // CGST = SGST = totalGst / 2
    @Column(precision = 12, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO;

    // grandTotal = subtotal + totalGst
    @Column(name = "grand_total", precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private BillingStatus status = BillingStatus.PENDING;

    public enum BillingStatus { PENDING, PAID, CANCELLED }

    // ─── Constructors ────────────────────────────────────────────────

    public Billing() {}

    // ─── Helpers ────────────────────────────────────────────────────

    public void addItem(BillingItem item) {
        items.add(item);
        item.setBilling(this);
    }

    /**
     * Recompute all bill-level totals from line items.
     * Call this after all items are finalised.
     */
    public void recalculateTotals() {
        subtotal   = BigDecimal.ZERO;
        totalGst   = BigDecimal.ZERO;

        for (BillingItem item : items) {
            if (item.getItemTotal()  != null) subtotal = subtotal.add(item.getItemTotal());
            if (item.getGstAmount()  != null) totalGst = totalGst.add(item.getGstAmount());
        }

        cgst       = totalGst.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        sgst       = cgst;
        grandTotal = subtotal.add(totalGst);
    }

    // ─── Getters & Setters ───────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public List<BillingItem> getItems() { return items; }
    public void setItems(List<BillingItem> items) { this.items = items; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTotalGst() { return totalGst; }
    public void setTotalGst(BigDecimal totalGst) { this.totalGst = totalGst; }

    public BigDecimal getCgst() { return cgst; }
    public void setCgst(BigDecimal cgst) { this.cgst = cgst; }

    public BigDecimal getSgst() { return sgst; }
    public void setSgst(BigDecimal sgst) { this.sgst = sgst; }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }

    public BillingStatus getStatus() { return status; }
    public void setStatus(BillingStatus status) { this.status = status; }
}