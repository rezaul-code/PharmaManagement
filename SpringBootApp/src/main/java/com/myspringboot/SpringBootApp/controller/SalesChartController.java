package com.myspringboot.SpringBootApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * REST endpoint that returns sales chart data for a given period.
 * Called by the dashboard JS via fetch() when the user clicks a period button.
 *
 * GET /api/dashboard/sales-chart?period=7d | 30d | 90d | 1y
 *
 * Response: JSON array of { label, revenue }
 *   - 7d  → one point per day  (last 7 days)
 *   - 30d → one point per day  (last 30 days)
 *   - 90d → one point per week (last 13 weeks, ~91 days)
 *   - 1y  → one point per month (last 12 months)
 */
@RestController
@RequestMapping("/api/dashboard")
public class SalesChartController {

    @Autowired private BillingRepository     billingRepository;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping("/sales-chart")
    public String salesChart(@RequestParam(defaultValue = "7d") String period) {
        try {
            Long pharmacyId = tenantPharmacyService.getCurrentPharmacy() != null
                    ? tenantPharmacyService.getCurrentPharmacy().getId()
                    : null;

            if (pharmacyId == null) return "[]";

            return switch (period) {
                case "30d" -> buildDailyData(pharmacyId, 30);
                case "90d" -> buildWeeklyData(pharmacyId);
                case "1y"  -> buildMonthlyData(pharmacyId);
                default    -> buildDailyData(pharmacyId, 7);   // "7d"
            };
        } catch (Exception e) {
            return "[]";
        }
    }

    // ── Daily (7d or 30d) ─────────────────────────────────────────────────────
    private String buildDailyData(Long pharmacyId, int days) throws Exception {
        LocalDateTime end   = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        for (Object[] row : billingRepository.findDailyRevenueBetween(pharmacyId, start, end)) {
            LocalDate day = toLocalDate(row[0]);
            revenueByDate.put(day, toBigDecimal(row[1]));
        }

        // Label format: shorter for 30d to avoid clutter
        DateTimeFormatter fmt = (days <= 7)
                ? DateTimeFormatter.ofPattern("EEE d")   // "Mon 9"
                : DateTimeFormatter.ofPattern("d MMM");  // "9 Mar"

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("label",   day.format(fmt));
            pt.put("revenue", revenueByDate.getOrDefault(day, BigDecimal.ZERO));
            result.add(pt);
        }
        return MAPPER.writeValueAsString(result);
    }

    // ── Weekly (90d → 13 complete weeks) ─────────────────────────────────────
    private String buildWeeklyData(Long pharmacyId) throws Exception {
        // Pull 91 days of daily data then sum into ISO weeks
        LocalDateTime end   = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(90).atStartOfDay();

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        for (Object[] row : billingRepository.findDailyRevenueBetween(pharmacyId, start, end)) {
            revenueByDate.put(toLocalDate(row[0]), toBigDecimal(row[1]));
        }

        // Build 13-week buckets (week starts on Monday)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        List<Map<String, Object>> result = new ArrayList<>();

        // Start from the Monday of the week 12 weeks ago
        LocalDate monday = LocalDate.now().minusWeeks(12);
        monday = monday.minusDays(monday.getDayOfWeek().getValue() - 1); // back to Monday

        for (int w = 0; w < 13; w++) {
            LocalDate weekStart = monday.plusWeeks(w);
            LocalDate weekEnd   = weekStart.plusDays(6);

            BigDecimal total = BigDecimal.ZERO;
            for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                total = total.add(revenueByDate.getOrDefault(d, BigDecimal.ZERO));
            }

            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("label",   weekStart.format(fmt));   // e.g. "10 Dec"
            pt.put("revenue", total);
            result.add(pt);
        }
        return MAPPER.writeValueAsString(result);
    }

    // ── Monthly (1y → 12 months) ──────────────────────────────────────────────
    private String buildMonthlyData(Long pharmacyId) throws Exception {
        LocalDateTime end   = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();

        // findMonthlyRevenue returns Object[]: [yr, mo, revenue, profit, units]
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (Object[] row : billingRepository.findMonthlyRevenue(pharmacyId, start, end)) {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            String key = yr + "-" + String.format("%02d", mo);
            revenueByMonth.put(key, toBigDecimal(row[2]));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy"); // "Mar 25"
        List<Map<String, Object>> result = new ArrayList<>();

        YearMonth cursor = YearMonth.now().minusMonths(11);
        for (int m = 0; m < 12; m++) {
            YearMonth ym  = cursor.plusMonths(m);
            String    key = ym.getYear() + "-" + String.format("%02d", ym.getMonthValue());

            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("label",   ym.atDay(1).format(fmt));
            pt.put("revenue", revenueByMonth.getOrDefault(key, BigDecimal.ZERO));
            result.add(pt);
        }
        return MAPPER.writeValueAsString(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static LocalDate toLocalDate(Object o) {
        if (o instanceof Date)      return ((Date) o).toLocalDate();
        if (o instanceof LocalDate) return (LocalDate) o;
        return LocalDate.parse(o.toString());
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }
}