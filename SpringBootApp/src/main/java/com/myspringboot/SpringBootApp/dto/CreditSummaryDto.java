// src/main/java/com/myspringboot/SpringBootApp/dto/CreditSummaryDto.java
package com.myspringboot.SpringBootApp.dto;

import java.math.BigDecimal;

public class CreditSummaryDto {

    private BigDecimal totalCreditIssued  = BigDecimal.ZERO;
    private BigDecimal totalRecovered     = BigDecimal.ZERO;
    private BigDecimal pendingBalance     = BigDecimal.ZERO;
    private long       totalCreditBills   = 0;
    private long       pendingCreditBills = 0;

    public CreditSummaryDto() {}

    public CreditSummaryDto(BigDecimal totalCreditIssued,
                             BigDecimal totalRecovered,
                             BigDecimal pendingBalance,
                             long totalCreditBills,
                             long pendingCreditBills) {
        this.totalCreditIssued  = totalCreditIssued  != null ? totalCreditIssued  : BigDecimal.ZERO;
        this.totalRecovered     = totalRecovered     != null ? totalRecovered     : BigDecimal.ZERO;
        this.pendingBalance     = pendingBalance     != null ? pendingBalance     : BigDecimal.ZERO;
        this.totalCreditBills   = totalCreditBills;
        this.pendingCreditBills = pendingCreditBills;
    }

    public BigDecimal getTotalCreditIssued()  { return totalCreditIssued; }
    public BigDecimal getTotalRecovered()     { return totalRecovered; }
    public BigDecimal getPendingBalance()     { return pendingBalance; }
    public long       getTotalCreditBills()   { return totalCreditBills; }
    public long       getPendingCreditBills() { return pendingCreditBills; }

    public void setTotalCreditIssued(BigDecimal v)  { this.totalCreditIssued  = v; }
    public void setTotalRecovered(BigDecimal v)     { this.totalRecovered     = v; }
    public void setPendingBalance(BigDecimal v)     { this.pendingBalance     = v; }
    public void setTotalCreditBills(long v)         { this.totalCreditBills   = v; }
    public void setPendingCreditBills(long v)       { this.pendingCreditBills = v; }
}