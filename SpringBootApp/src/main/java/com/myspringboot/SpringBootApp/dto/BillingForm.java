// src/main/java/com/myspringboot/SpringBootApp/dto/BillingForm.java
package com.myspringboot.SpringBootApp.dto;

import com.myspringboot.SpringBootApp.model.PaymentType;
import com.myspringboot.SpringBootApp.model.BillingItemForm;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BillingForm {

    private String patientName;
    private String patientPhone;
    private String notes;

    private List<BillingItemForm> items = new ArrayList<>();

    // Totals (populated by JS, used server-side as reference only)
    private BigDecimal subtotal   = BigDecimal.ZERO;
    private BigDecimal totalGst   = BigDecimal.ZERO;
    private BigDecimal cgst       = BigDecimal.ZERO;
    private BigDecimal sgst       = BigDecimal.ZERO;
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // ── New credit fields ──────────────────────────────────────────────
    private PaymentType paymentType = PaymentType.CASH;

    @DecimalMin(value = "0.0", message = "Initial payment cannot be negative")
    private BigDecimal initialPayment = BigDecimal.ZERO;

    // Getters / Setters

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<BillingItemForm> getItems() { return items; }
    public void setItems(List<BillingItemForm> items) { this.items = items; }

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

    public BigDecimal getInitialPayment() { return initialPayment; }
    public void setInitialPayment(BigDecimal initialPayment) { this.initialPayment = initialPayment; }
}