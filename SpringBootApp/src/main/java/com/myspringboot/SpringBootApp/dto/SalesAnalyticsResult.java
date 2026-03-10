package com.myspringboot.SpringBootApp.dto;

import java.math.BigDecimal;

/**
 * Projection returned by BillingRepository analytics queries.
 *
 * Revenue = SUM(bi.quantity × bi.unitPrice)
 * Profit  = SUM(bi.quantity × (m.price − m.purchasePrice))
 *           joined via bi.medicine → Medicine.price / Medicine.purchasePrice
 */
public class SalesAnalyticsResult {

    private final String     medicineName;
    private final long       totalQuantity;
    private final BigDecimal totalRevenue;
    private final BigDecimal totalProfit;

    public SalesAnalyticsResult(String medicineName,
                                Long totalQuantity,
                                BigDecimal totalRevenue,
                                BigDecimal totalProfit) {
        this.medicineName  = medicineName;
        this.totalQuantity = totalQuantity  != null ? totalQuantity  : 0L;
        this.totalRevenue  = totalRevenue   != null ? totalRevenue   : BigDecimal.ZERO;
        this.totalProfit   = totalProfit    != null ? totalProfit    : BigDecimal.ZERO;
    }

    public String     getMedicineName()  { return medicineName;  }
    public long       getTotalQuantity() { return totalQuantity; }
    public BigDecimal getTotalRevenue()  { return totalRevenue;  }
    public BigDecimal getTotalProfit()   { return totalProfit;   }
}