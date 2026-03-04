package com.myspringboot.SpringBootApp.dto;

import com.myspringboot.SpringBootApp.model.BillingItemForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * Top-level form submitted when creating a new bill.
 *
 * Merges your original BillingForm (customerName, @Valid @NotEmpty items)
 * with the v2.0 upgrade fields (patientPhone, GST summary totals).
 *
 * customerName is kept as an alias so any existing Thymeleaf templates
 * that bind to "customerName" still work without changes.
 */
public class BillingForm {

    // ── Patient / customer info ──────────────────────────────────────

    // Primary field used in upgraded templates
    private String patientName;

    // Kept from your original BillingForm — maps to the same concept.
    // If your existing billing_new.html binds to "customerName", this
    // field still binds correctly without any template change needed.
    private String customerName;

    private String patientPhone;

    // ── Line items ───────────────────────────────────────────────────

    @Valid
    @NotEmpty(message = "Add at least one item")
    private List<BillingItemForm> items = new ArrayList<>();

    // ── Bill-level GST summary ───────────────────────────────────────
    // Populated by JS on the client; recalculated server-side before save.

    private BigDecimal subtotal   = BigDecimal.ZERO;
    private BigDecimal totalGst   = BigDecimal.ZERO;
    private BigDecimal cgst       = BigDecimal.ZERO;
    private BigDecimal sgst       = BigDecimal.ZERO;
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // ─── Getters & Setters ───────────────────────────────────────────

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) {
        this.patientName = patientName;
        // Keep customerName in sync so both field names resolve correctly
        this.customerName = patientName;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
        // Keep patientName in sync
        this.patientName = customerName;
    }

    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }

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
}