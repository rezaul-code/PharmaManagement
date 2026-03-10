package com.myspringboot.SpringBootApp.dto;

import java.math.BigDecimal;

/**
 * Immutable projection returned by the analytics queries.
 * One row = one medicine with its aggregated sales figures.
 */
public class SalesAnalyticsResult {

    private final Long   medicineId;
    private final String medicineName;
    private final Long   totalQuantity;
    private final BigDecimal totalRevenue;
    private final BigDecimal totalProfit;

    /** JPQL constructor expression — parameter order must match exactly. */
    public SalesAnalyticsResult(Long   medicineId,
                                String medicineName,
                                Long   totalQuantity,
                                BigDecimal totalRevenue,
                                BigDecimal totalProfit) {
        this.medicineId    = medicineId;
        this.medicineName  = medicineName;
        this.totalQuantity = totalQuantity != null ? totalQuantity : 0L;
        this.totalRevenue  = totalRevenue  != null ? totalRevenue  : BigDecimal.ZERO;
        this.totalProfit   = totalProfit   != null ? totalProfit   : BigDecimal.ZERO;
    }

    public Long       getMedicineId()   { return medicineId;   }
    public String     getMedicineName() { return medicineName; }
    public Long       getTotalQuantity(){ return totalQuantity;}
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getTotalProfit()  { return totalProfit;  }
}