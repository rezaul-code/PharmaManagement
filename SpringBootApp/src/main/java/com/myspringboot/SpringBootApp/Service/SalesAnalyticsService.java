package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // ── Date range helpers ────────────────────────────────────────────────

    /** Returns start-of-day for N days ago. */
    private LocalDateTime daysAgo(int days) {
        return LocalDate.now().minusDays(days).atStartOfDay();
    }

    private LocalDateTime startOf(String period) {
        return switch (period) {
            case "7d"  -> daysAgo(7);
            case "90d" -> daysAgo(90);
            case "1y"  -> daysAgo(365);
            default    -> daysAgo(30);   // "30d" is the default
        };
    }

    private LocalDateTime endOfToday() {
        return LocalDate.now().plusDays(1).atStartOfDay();
    }

    // ── Top-10 queries ────────────────────────────────────────────────────

    /**
     * Returns top 10 medicines for the given metric and period.
     *
     * @param metric "quantity" | "revenue" | "profit"
     * @param period "7d" | "30d" | "90d" | "1y"
     */
    public List<SalesAnalyticsResult> getTopMedicines(String metric, String period) {
        Long          pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime start      = startOf(period);
        LocalDateTime end        = endOfToday();

        List<SalesAnalyticsResult> raw = switch (metric) {
            case "revenue" -> billingRepository.findTopByRevenue(pharmacyId, start, end);
            case "profit"  -> billingRepository.findTopByProfit (pharmacyId, start, end);
            default        -> billingRepository.findTopByQuantity(pharmacyId, start, end);
        };

        return raw.stream().limit(TOP_N).toList();
    }

    // ── Summary stats ─────────────────────────────────────────────────────

    /**
     * Totals across ALL medicines (not just top-10) for the period.
     * Used in the stat cards at the top of the page.
     */
    public Map<String, BigDecimal> getSummary(String period) {
        Long          pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime start      = startOf(period);
        LocalDateTime end        = endOfToday();

        // Re-use the quantity query — it returns every medicine; we just aggregate
        List<SalesAnalyticsResult> all =
                billingRepository.findTopByQuantity(pharmacyId, start, end);

        BigDecimal totalRevenue  = all.stream()
                .map(SalesAnalyticsResult::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfit   = all.stream()
                .map(SalesAnalyticsResult::getTotalProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalQty = all.stream()
                .mapToLong(SalesAnalyticsResult::getTotalQuantity)
                .sum();

        Map<String, BigDecimal> summary = new LinkedHashMap<>();
        summary.put("totalRevenue",  totalRevenue);
        summary.put("totalProfit",   totalProfit);
        summary.put("totalQuantity", BigDecimal.valueOf(totalQty));
        summary.put("totalMedicines",BigDecimal.valueOf(all.size()));
        return summary;
    }

    // ── Daily revenue trend (for line chart) ─────────────────────────────

    /**
     * Returns a map of { "YYYY-MM-DD" → revenue } ordered by date.
     * Used to render the daily revenue trend line chart.
     */
    public Map<String, BigDecimal> getDailyRevenueTrend(String period) {
        Long          pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        LocalDateTime start      = startOf(period);
        LocalDateTime end        = endOfToday();

        List<Object[]> rows =
                billingRepository.findDailyRevenueBetween(pharmacyId, start, end);

        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String     day     = row[0].toString();   // DATE string
            BigDecimal revenue = (BigDecimal) row[1];
            trend.put(day, revenue);
        }
        return trend;
    }
}