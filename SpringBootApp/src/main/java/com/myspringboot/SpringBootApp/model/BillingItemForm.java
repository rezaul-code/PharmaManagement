package com.myspringboot.SpringBootApp.model;

import java.math.BigDecimal;

/**
 * Form-backing object for a single billing line item.
 * Submitted as part of BillingForm (list of items).
 */
public class BillingItemForm {

    private Long medicineId;
    private String medicineName;
    private String batchNo;
    private Integer quantity;

    private BigDecimal unitPrice;

    // GST % (e.g. 12.00)
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    // ─── Computed helpers (populated by JS / backend) ────────────────

    // itemTotal = unitPrice * quantity
    private BigDecimal itemTotal;

    // gstAmount = itemTotal * gstPercentage / 100
    private BigDecimal gstAmount;

    // totalAmount = itemTotal + gstAmount
    private BigDecimal totalAmount;

    // ─── Getters & Setters ───────────────────────────────────────────

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }

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