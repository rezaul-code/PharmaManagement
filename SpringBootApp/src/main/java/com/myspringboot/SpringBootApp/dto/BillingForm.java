package com.myspringboot.SpringBootApp.dto;

import com.myspringboot.SpringBootApp.model.BillingItemForm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BillingForm {

    // ── Patient / customer info ──────────────────────────────────────
    private String patientName;
    private String customerName;
    private String patientPhone;
    private String notes;

    // ── Line items ───────────────────────────────────────────────────
    @Valid
    @NotEmpty(message = "Add at least one item")
    private List<BillingItemForm> items = new ArrayList<>();

    // ── Bill-level GST summary ───────────────────────────────────────
    private BigDecimal subtotal   = BigDecimal.ZERO;
    private BigDecimal totalGst   = BigDecimal.ZERO;
    private BigDecimal cgst       = BigDecimal.ZERO;
    private BigDecimal sgst       = BigDecimal.ZERO;
    private BigDecimal grandTotal = BigDecimal.ZERO;

    // ─── Getters & Setters ───────────────────────────────────────────
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) {
        this.patientName = patientName;
        this.customerName = patientName;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
        this.patientName = customerName;
    }

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
}