package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.SalesAnalyticsService;
import com.myspringboot.SpringBootApp.dto.MonthlyTrendResult;
import com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class SalesAnalyticsController {

    @Autowired
    private SalesAnalyticsService salesAnalyticsService;

    @Autowired
    private MedicineService medicineService;

    // ════════════════════════════════════════════════════════════════════════
    // 1. GET /sales-analytics — Top medicines + daily trend + summary cards
    // ════════════════════════════════════════════════════════════════════════

    /**
     * @param metric \"quantity\" | \"revenue\" | \"profit\"  (default: quantity)
     * @param period \"7d\" | \"30d\" | \"90d\" | \"1y\"        (default: 30d)
     * @param topN   5 | 10 | 20                          (default: 5)
     */
    @GetMapping("/sales-analytics")
    public String salesAnalytics(
            @RequestParam(defaultValue = "quantity") String metric,
            @RequestParam(defaultValue = "30d")      String period,
            @RequestParam(defaultValue = "5")        int topN,
            HttpSession session,
            Model model) {

        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";

        // Clamp topN to safe values
        if (topN != 5 && topN != 10 && topN != 20) topN = 5;

        List<SalesAnalyticsResult> topMedicines =
                salesAnalyticsService.getTopMedicines(metric, period, topN);
        Map<String, BigDecimal> summary =
                salesAnalyticsService.getSummary(period);
        Map<String, BigDecimal> dailyTrend =
                salesAnalyticsService.getDailyRevenueTrend(period);

        // ── Chart data as proper List objects ──────────────────────────────
        // Thymeleaf's /*[[${list}]]*/ serialises List objects as real JS arrays.
        // Passing a String that *looks* like JSON causes Thymeleaf to quote-wrap
        // it, turning ["a","b"] into the string '["a","b"]'. Chart.js then
        // iterates character-by-character → vertical letter stacking.
        List<String>     chartLabels  = new ArrayList<>();
        List<Long>       chartQty     = new ArrayList<>();
        List<BigDecimal> chartRevenue = new ArrayList<>();
        List<BigDecimal> chartProfit  = new ArrayList<>();
        List<BigDecimal> chartValues  = new ArrayList<>();

        for (SalesAnalyticsResult r : topMedicines) {
            chartLabels .add(r.getMedicineName());
            chartQty    .add(r.getTotalQuantity());
            chartRevenue.add(r.getTotalRevenue());
            chartProfit .add(r.getTotalProfit());

            BigDecimal val = switch (metric) {
                case "revenue" -> r.getTotalRevenue();
                case "profit"  -> r.getTotalProfit();
                default        -> BigDecimal.valueOf(r.getTotalQuantity());
            };
            chartValues.add(val);
        }

        // ── Daily trend data as proper Lists ──────────────────────────────
        List<String>     trendLabels = new ArrayList<>();
        List<BigDecimal> trendValues = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : dailyTrend.entrySet()) {
            trendLabels.add(e.getKey());
            trendValues.add(e.getValue());
        }

        model.addAttribute("topMedicines",  topMedicines);
        model.addAttribute("summary",       summary);
        model.addAttribute("metric",        metric);
        model.addAttribute("period",        period);
        model.addAttribute("topN",          topN);
        model.addAttribute("chartLabels",   chartLabels);
        model.addAttribute("chartValues",   chartValues);
        model.addAttribute("chartQty",      chartQty);
        model.addAttribute("chartRevenue",  chartRevenue);
        model.addAttribute("chartProfit",   chartProfit);
        model.addAttribute("trendLabels",   trendLabels);
        model.addAttribute("trendValues",   trendValues);
        model.addAttribute("activePage",    "sales-analytics");

        return "pages/sales-analytics";
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. GET /inventory-analytics — Inventory health cards
    // ════════════════════════════════════════════════════════════════════════

    @GetMapping("/inventory-analytics")
    public String inventoryAnalytics(HttpSession session, Model model) {

        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";

        model.addAttribute("totalCostValue",      medicineService.getTotalInventoryCostValue());
        model.addAttribute("totalSellingValue",   medicineService.getTotalInventorySellingValue());
        model.addAttribute("potentialProfit",     medicineService.getPotentialInventoryProfit());
        model.addAttribute("negativeMarginCount", medicineService.getNegativeMarginCount());
        model.addAttribute("activePage",          "inventory-analytics");

        return "pages/inventory-analytics";
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. GET /monthly-trends — Monthly/daily revenue + profit trend
    // ════════════════════════════════════════════════════════════════════════

    /**
     * @param period "15d" | "1m" | "6m" | "12m"        (default: 6m)
     * @param year   e.g. 2026   — if set with month, overrides period
     * @param month  1–12        — if set with year, overrides period
     */
    @GetMapping("/monthly-trends")
    public String monthlyTrends(
            @RequestParam(defaultValue = "6m") String period,
            @RequestParam(required = false)    Integer year,
            @RequestParam(required = false)    Integer month,
            HttpSession session,
            Model model) {

        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";

        List<MonthlyTrendResult> trendData;
        String viewMode;        // "daily" or "monthly"
        String activePeriod;    // which quick-filter is active
        String periodLabel;     // human label for KPI card subtitle
        Integer selectedYear  = null;
        Integer selectedMonth = null;

        LocalDate today = LocalDate.now();

        // ── Specific month mode ─────────────────────────────────────────────
        if (year != null && month != null) {
            // Validate — fallback to current month if invalid
            if (year < 2000 || year > today.getYear() + 1 || month < 1 || month > 12) {
                year  = today.getYear();
                month = today.getMonthValue();
            }
            selectedYear  = year;
            selectedMonth = month;

            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDateTime start  = monthStart.atStartOfDay();
            LocalDateTime end    = monthStart.plusMonths(1).atStartOfDay();

            trendData    = salesAnalyticsService.getDailyTrend(start, end);
            viewMode     = "daily";
            activePeriod = "custom";

            // e.g. "March 2026"
            String monthName = monthStart.getMonth().name();
            monthName = monthName.charAt(0) + monthName.substring(1).toLowerCase();
            periodLabel = monthName + " " + year;
        }
        // ── Quick-filter period mode ────────────────────────────────────────
        else {
            activePeriod = period;
            switch (period) {
                case "15d" -> {
                    LocalDateTime start = today.minusDays(15).atStartOfDay();
                    LocalDateTime end   = today.plusDays(1).atStartOfDay();
                    trendData   = salesAnalyticsService.getDailyTrend(start, end);
                    viewMode    = "daily";
                    periodLabel = "Last 15 Days";
                }
                case "1m" -> {
                    LocalDateTime start = today.minusMonths(1).atStartOfDay();
                    LocalDateTime end   = today.plusDays(1).atStartOfDay();
                    trendData   = salesAnalyticsService.getDailyTrend(start, end);
                    viewMode    = "daily";
                    periodLabel = "Last 1 Month";
                }
                case "12m" -> {
                    trendData   = salesAnalyticsService.getMonthlyTrend(12);
                    viewMode    = "monthly";
                    periodLabel = "Last 12 Months";
                }
                default -> { // "6m" and any unknown value
                    activePeriod = "6m";
                    trendData    = salesAnalyticsService.getMonthlyTrend(6);
                    viewMode     = "monthly";
                    periodLabel  = "Last 6 Months";
                }
            }
        }

        // ── Chart data as Lists (Thymeleaf serialises correctly) ────────────
        List<String>     mLabels  = new ArrayList<>();
        List<BigDecimal> mRevenue = new ArrayList<>();
        List<BigDecimal> mProfit  = new ArrayList<>();
        List<Long>       mUnits   = new ArrayList<>();

        for (MonthlyTrendResult r : trendData) {
            mLabels .add(r.getMonthLabel());
            mRevenue.add(r.getRevenue());
            mProfit .add(r.getProfit());
            mUnits  .add(r.getUnits());
        }

        // Year list for the dropdown (last 3 years)
        int currentYear = today.getYear();
        List<Integer> yearOptions = List.of(currentYear - 2, currentYear - 1, currentYear);

        model.addAttribute("monthlyTrend",   trendData);
        model.addAttribute("monthlyLabels",  mLabels);
        model.addAttribute("monthlyRevenue", mRevenue);
        model.addAttribute("monthlyProfit",  mProfit);
        model.addAttribute("monthlyUnits",   mUnits);
        model.addAttribute("viewMode",       viewMode);
        model.addAttribute("activePeriod",   activePeriod);
        model.addAttribute("periodLabel",    periodLabel);
        model.addAttribute("selectedYear",   selectedYear);
        model.addAttribute("selectedMonth",  selectedMonth);
        model.addAttribute("yearOptions",    yearOptions);
        model.addAttribute("activePage",     "monthly-trends");

        return "pages/monthly-trends";
    }
}