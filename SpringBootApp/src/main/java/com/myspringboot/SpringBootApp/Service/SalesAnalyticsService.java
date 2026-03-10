package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.dto.MonthlyTrendResult;
import com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalesAnalyticsService {

    private static final int TOP_N = 10;

    @Autowired
    private BillingRepository billingRepository;

    @Autowired
    private TenantPharmacyService tenantPharmacyService;

    // ── Date-range helpers ────────────────────────────────────────────────────

    private LocalDateTime daysAgo(int days) {
        return LocalDate.now().minusDays(days).atStartOfDay();
    }

    private LocalDateTime startOf(String period) {
        return switch (period) {
            case "7d"  -> daysAgo(7);
            case "90d" -> daysAgo(90);
            case "1y"  -> daysAgo(365);
            default    -> daysAgo(30);  // "30d"
        };
    }

    private LocalDateTime endOfToday() {
        return LocalDate.now().plusDays(1).atStartOfDay();
    }

    // ── Top-10 medicines ──────────────────────────────────────────────────────

    /**
     * Top 10 medicines sorted by the chosen metric for the given period.
     *
     * @param metric "quantity" | "revenue" | "profit"
     * @param period "7d" | "30d" | "90d" | "1y"
     */
    public List<SalesAnalyticsResult> getTopMedicines(String metric, String period) {
        Long          pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime start      = startOf(period);
        LocalDateTime end        = endOfToday();

        List<SalesAnalyticsResult> raw = switch (metric) {
            case "revenue" -> billingRepository.findTopByRevenue (pharmacyId, start, end);
            case "profit"  -> billingRepository.findTopByProfit  (pharmacyId, start, end);
            default        -> billingRepository.findTopByQuantity(pharmacyId, start, end);
        };

        return raw.stream().limit(TOP_N).toList();
    }

    // ── Period-wide summary stat cards ───────────────────────────────────────

    /**
     * Totals across ALL medicines (not just top-10) for the period.
     * Keys: totalRevenue, totalProfit, totalQuantity, totalMedicines
     */
    public Map<String, BigDecimal> getSummary(String period) {
        Long          pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime start      = startOf(period);
        LocalDateTime end        = endOfToday();

        // Re-use quantity query — it returns every medicine with sales
        List<SalesAnalyticsResult> all =
                billingRepository.findTopByQuantity(pharmacyId, start, end);

        BigDecimal totalRevenue = all.stream()
                .map(SalesAnalyticsResult::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit = all.stream()
                .map(SalesAnalyticsResult::getTotalProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalQty = all.stream()
                .mapToLong(SalesAnalyticsResult::getTotalQuantity)
                .sum();

        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        summary.put("totalRevenue",   totalRevenue);
        summary.put("totalProfit",    totalProfit);
        summary.put("totalQuantity",  BigDecimal.valueOf(totalQty));
        summary.put("totalMedicines", BigDecimal.valueOf(all.size()));
        return summary;
    }

    // ── Daily revenue trend ───────────────────────────────────────────────────

    /**
     * Map of { "YYYY-MM-DD" → daily revenue } ordered by date.
     * Used by the small "Daily Revenue Trend" line chart.
     * Uses b.createdAt (confirmed field on Billing entity).
     */
    public Map<String, BigDecimal> getDailyRevenueTrend(String period) {
        Long          pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime start      = startOf(period);
        LocalDateTime end        = endOfToday();

        List<Object[]> rows =
                billingRepository.findDailyRevenueBetween(pharmacyId, start, end);

        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String     day     = row[0].toString();
            BigDecimal revenue = (BigDecimal) row[1];
            trend.put(day, revenue);
        }
        return trend;
    }

    // ── Monthly sales trend  (NEW) ────────────────────────────────────────────

    /**
     * Returns one {@link MonthlyTrendResult} per calendar month for the
     * last {@code months} months (6 or 12), ordered oldest → newest.
     *
     * <p>Revenue  = SUM(bi.quantity × bi.unitPrice)
     * <p>Profit   = SUM(bi.quantity × (m.price − m.purchasePrice))
     *               — NULLs handled by COALESCE in the repository query
     * <p>Units    = SUM(bi.quantity)
     *
     * @param months 6 or 12 (anything else is treated as 6)
     */
    public List<MonthlyTrendResult> getMonthlyTrend(int months) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        // Start from the 1st of the month N months ago
        LocalDateTime start = LocalDate.now()
                                       .minusMonths(months)
                                       .withDayOfMonth(1)
                                       .atStartOfDay();
        LocalDateTime end   = endOfToday();

        List<Object[]> rows =
                billingRepository.findMonthlyRevenue(pharmacyId, start, end);

        // Short month names indexed 1–12
        final String[] MONTH_NAMES = {
            "", "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
        };

        List<MonthlyTrendResult> result = new ArrayList<>();
        for (Object[] row : rows) {
            int        yr      = ((Number) row[0]).intValue();
            int        mo      = ((Number) row[1]).intValue();
            // COALESCE in SQL ensures these are never null, but guard anyway
            BigDecimal revenue = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            BigDecimal profit  = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            long       units   = row[4] != null ? ((Number) row[4]).longValue() : 0L;

            String label = MONTH_NAMES[mo] + " " + yr;  // e.g. "Mar 2025"
            result.add(new MonthlyTrendResult(label, revenue, profit, units));
        }
        return result;
    }
}