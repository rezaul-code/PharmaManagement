package com.myspringboot.SpringBootApp.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "billing_items")
public class BillingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    private Billing billing;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    private String medicineName;   // snapshot at time of billing
    private String batchNo;        // snapshot at time of billing

    @Column(nullable = false)
    private Integer quantity;

    // Unit price at time of billing
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // GST percentage at time of billing
    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    // itemTotal = unitPrice * quantity
    @Column(name = "item_total", precision = 10, scale = 2)
    private BigDecimal itemTotal;

    // gstAmount = itemTotal * gstPercentage / 100
    @Column(name = "gst_amount", precision = 10, scale = 2)
    private BigDecimal gstAmount;

    // totalAmount = itemTotal + gstAmount
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // ─── Constructors ────────────────────────────────────────────────

    public BillingItem() {}

    // ─── Business Logic ──────────────────────────────────────────────

    /**
     * Call after setting unitPrice, quantity, gstPercentage
     * to compute derived totals.
     */
    public void calculateTotals() {
        if (unitPrice == null || quantity == null) return;

        itemTotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                             .setScale(2, RoundingMode.HALF_UP);

        BigDecimal gstPct = (gstPercentage != null) ? gstPercentage : BigDecimal.ZERO;
        gstAmount = itemTotal.multiply(gstPct)
                             .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        totalAmount = itemTotal.add(gstAmount).setScale(2, RoundingMode.HALF_UP);
    }

    // CGST = SGST = gstAmount / 2
    public BigDecimal getCgst() {
        if (gstAmount == null) return BigDecimal.ZERO;
        return gstAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getSgst() {
        return getCgst();
    }

    // ─── Getters & Setters ───────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Billing getBilling() { return billing; }
    public void setBilling(Billing billing) { this.billing = billing; }

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getGstPercentage() { return gstPercentage; }
    public void setGstPercentage(BigDecimal gstPercentage) { this.gstPercentage = gstPercentage; }

    public BigDecimal getItemTotal() { return itemTotal; }
    public void setItemTotal(BigDecimal itemTotal) { this.itemTotal = itemTotal; }

    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}