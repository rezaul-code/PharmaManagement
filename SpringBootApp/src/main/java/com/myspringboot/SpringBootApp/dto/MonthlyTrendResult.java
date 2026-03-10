package com.myspringboot.SpringBootApp.dto;

import java.math.BigDecimal;

/**
 * Holds one calendar month's aggregated sales figures.
 * Produced by SalesAnalyticsService.getMonthlyTrend().
 *
 * Revenue = SUM(bi.quantity × bi.unitPrice)
 * Profit  = SUM(bi.quantity × (m.price − m.purchasePrice))
 * Units   = SUM(bi.quantity)
 */
public class MonthlyTrendResult {

    private final String     monthLabel;   // e.g. "Jan 2025"
    private final BigDecimal revenue;
    private final BigDecimal profit;
    private final long       units;

    public MonthlyTrendResult(String monthLabel,
                              BigDecimal revenue,
                              BigDecimal profit,
                              long units) {
        this.monthLabel = monthLabel;
        this.revenue    = revenue != null ? revenue : BigDecimal.ZERO;
        this.profit     = profit  != null ? profit  : BigDecimal.ZERO;
        this.units      = units;
    }

    public String     getMonthLabel() { return monthLabel; }
    public BigDecimal getRevenue()    { return revenue;    }
    public BigDecimal getProfit()     { return profit;     }
    public long       getUnits()      { return units;      }
}