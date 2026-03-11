// src/main/java/com/myspringboot/SpringBootApp/model/Billing.java
package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "billings",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_billings_pharmacy_bill_number",
                          columnNames = {"pharmacy_id", "bill_number"})
    }
)
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number")
    private String billNumber;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_phone")
    private String patientPhone;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillingItem> items = new ArrayList<>();

    // ── Credit payments (new) ──────────────────────────────────────────
    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditPayment> creditPayments = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "total_gst", precision = 12, scale = 2)
    private BigDecimal totalGst = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal cgst = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal sgst = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // ── New payment / credit fields ────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 20, nullable = false)
    private PaymentType paymentType = PaymentType.CASH;

    @Column(name = "credit_amount", precision = 12, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 12, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private BillingStatus status = BillingStatus.PENDING;

    // ── Enums ──────────────────────────────────────────────────────────
    public enum BillingStatus {
        PENDING, PAID, CANCELLED, CREDIT_PENDING, CREDIT_PARTIAL, CREDIT_CLEARED
    }

    public Billing() {}

    // ── Domain methods ─────────────────────────────────────────────────

    public void addItem(BillingItem item) {
        items.add(item);
        item.setBilling(this);
    }

    public void recalculateTotals() {
        subtotal  = BigDecimal.ZERO;
        totalGst  = BigDecimal.ZERO;

        for (BillingItem item : items) {
            if (item.getItemTotal() != null) subtotal  = subtotal.add(item.getItemTotal());
            if (item.getGstAmount()  != null) totalGst  = totalGst.add(item.getGstAmount());
        }

        cgst       = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        sgst       = cgst;
        grandTotal = subtotal.add(totalGst);
    }

    /**
     * Sets credit/paid amounts and derives the correct BillingStatus.
     * Called by BillingService when paymentType == CREDIT.
     */
    public void applyPaymentType(PaymentType type, BigDecimal initialPayment) {
        this.paymentType = type;

        if (type == PaymentType.CREDIT) {
            BigDecimal paid = (initialPayment != null && initialPayment.compareTo(BigDecimal.ZERO) > 0)
                    ? initialPayment.min(grandTotal)
                    : BigDecimal.ZERO;

            this.paidAmount   = paid;
            this.creditAmount = grandTotal;
            this.balanceDue   = grandTotal.subtract(paid).max(BigDecimal.ZERO);

            if (balanceDue.compareTo(BigDecimal.ZERO) == 0) {
                this.status = BillingStatus.CREDIT_CLEARED;
            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                this.status = BillingStatus.CREDIT_PARTIAL;
            } else {
                this.status = BillingStatus.CREDIT_PENDING;
            }
        } else {
            this.paidAmount   = grandTotal;
            this.creditAmount = BigDecimal.ZERO;
            this.balanceDue   = BigDecimal.ZERO;
            this.status       = BillingStatus.PAID;
        }
    }

    /**
     * Records a new credit payment and refreshes balanceDue / status.
     */
    public void recordCreditPayment(CreditPayment cp) {
        creditPayments.add(cp);
        cp.setBilling(this);

        BigDecimal totalPaid = creditPayments.stream()
                .map(CreditPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.paidAmount = totalPaid.min(creditAmount);
        this.balanceDue = creditAmount.subtract(paidAmount).max(BigDecimal.ZERO);

        if (balanceDue.compareTo(BigDecimal.ZERO) == 0) {
            this.status = BillingStatus.CREDIT_CLEARED;
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = BillingStatus.CREDIT_PARTIAL;
        } else {
            this.status = BillingStatus.CREDIT_PENDING;
        }
    }

    public boolean isCreditBill() {
        return paymentType == PaymentType.CREDIT;
    }

    // ── Getters / Setters ──────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBillNumber() { return billNumber; }
    public void setBillNumber(String billNumber) { this.billNumber = billNumber; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Pharmacy getPharmacy() { return pharmacy; }
    public void setPharmacy(Pharmacy pharmacy) { this.pharmacy = pharmacy; }

    public List<BillingItem> getItems() { return items; }
    public void setItems(List<BillingItem> items) { this.items = items; }

    public List<CreditPayment> getCreditPayments() { return creditPayments; }
    public void setCreditPayments(List<CreditPayment> creditPayments) { this.creditPayments = creditPayments; }

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

    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }

    public BigDecimal getCreditAmount() { return creditAmount; }
    public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }

    public BillingStatus getStatus() { return status; }
    public void setStatus(BillingStatus status) { this.status = status; }
}