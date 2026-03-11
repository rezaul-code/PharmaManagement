package com.myspringboot.SpringBootApp.dto;

import com.myspringboot.SpringBootApp.model.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreditPaymentForm {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment mode is required")
    private PaymentType paymentMode = PaymentType.CASH;

    private String notes;

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public PaymentType getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentType paymentMode) { this.paymentMode = paymentMode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}