package com.myspringboot.SpringBootApp.controller.api;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.SalesAnalyticsService;
import com.myspringboot.SpringBootApp.dto.MonthlyTrendResult;
import com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsRestController {

    @Autowired private SalesAnalyticsService salesAnalyticsService;
    @Autowired private MedicineService medicineService;

    // ── GET /api/analytics/sales ──────────────────────────────────────────
    @GetMapping("/sales")
    public ResponseEntity<Map<String, Object>> getSalesAnalytics(
            @RequestParam(defaultValue = "quantity") String metric,
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(defaultValue = "5") int topN) {

        if (topN != 5 && topN != 10 && topN != 20) topN = 5;

        List<SalesAnalyticsResult> topMedicines = salesAnalyticsService.getTopMedicines(metric, period, topN);
        Map<String, BigDecimal> summary = salesAnalyticsService.getSummary(period);
        Map<String, BigDecimal> dailyTrend = salesAnalyticsService.getDailyRevenueTrend(period);

        Map<String, Object> data = new HashMap<>();
        data.put("topMedicines", topMedicines);
        data.put("summary", summary);
        data.put("dailyTrend", dailyTrend);
        data.put("metric", metric);
        data.put("period", period);
        data.put("topN", topN);

        return ResponseEntity.ok(data);
    }

    // ── GET /api/analytics/inventory ──────────────────────────────────────
    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventoryAnalytics() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalCostValue", medicineService.getTotalInventoryCostValue());
        data.put("totalSellingValue", medicineService.getTotalInventorySellingValue());
        data.put("potentialProfit", medicineService.getPotentialInventoryProfit());
        data.put("negativeMarginCount", medicineService.getNegativeMarginCount());
        return ResponseEntity.ok(data);
    }

    // ── GET /api/analytics/monthly-trends ─────────────────────────────────
    @GetMapping("/monthly-trends")
    public ResponseEntity<?> getMonthlyTrends(
            @RequestParam(defaultValue = "6m") String period,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        List<MonthlyTrendResult> trendData;
        LocalDate today = LocalDate.now();

        if (year != null && month != null) {
            if (year < 2000 || year > today.getYear() + 1 || month < 1 || month > 12) {
                year = today.getYear();
                month = today.getMonthValue();
            }
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDateTime start = monthStart.atStartOfDay();
            LocalDateTime end = monthStart.plusMonths(1).atStartOfDay();
            trendData = salesAnalyticsService.getDailyTrend(start, end);
        } else {
            switch (period) {
                case "15d" -> {
                    LocalDateTime start = today.minusDays(15).atStartOfDay();
                    LocalDateTime end = today.plusDays(1).atStartOfDay();
                    trendData = salesAnalyticsService.getDailyTrend(start, end);
                }
                case "1m" -> {
                    LocalDateTime start = today.minusMonths(1).atStartOfDay();
                    LocalDateTime end = today.plusDays(1).atStartOfDay();
                    trendData = salesAnalyticsService.getDailyTrend(start, end);
                }
                case "12m" -> {
                    trendData = salesAnalyticsService.getMonthlyTrend(12);
                }
                default -> { // "6m"
                    trendData = salesAnalyticsService.getMonthlyTrend(6);
                }
            }
        }
        return ResponseEntity.ok(trendData);
    }

    // ── GET /api/analytics/top-medicines ──────────────────────────────────
    @GetMapping("/top-medicines")
    public ResponseEntity<List<SalesAnalyticsResult>> getTopMedicines(
            @RequestParam(defaultValue = "quantity") String metric,
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(defaultValue = "5") int topN) {
        if (topN != 5 && topN != 10 && topN != 20) topN = 5;
        List<SalesAnalyticsResult> topMedicines = salesAnalyticsService.getTopMedicines(metric, period, topN);
        return ResponseEntity.ok(topMedicines);
    }

    // ── GET /api/analytics/profit ─────────────────────────────────────────
    @GetMapping("/profit")
    public ResponseEntity<Map<String, Object>> getProfitAnalytics() {
        Map<String, Object> data = new HashMap<>();
        data.put("potentialProfit", medicineService.getPotentialInventoryProfit());
        data.put("negativeMarginMedicines", medicineService.getNegativeMarginMedicines());
        data.put("topProfitMedicines", medicineService.getTopProfitMedicines(10));
        return ResponseEntity.ok(data);
    }

    // ── GET /api/analytics/revenue ────────────────────────────────────────
    @GetMapping("/revenue")
    public ResponseEntity<Map<String, BigDecimal>> getRevenueTrend(@RequestParam(defaultValue = "30d") String period) {
        Map<String, BigDecimal> dailyTrend = salesAnalyticsService.getDailyRevenueTrend(period);
        return ResponseEntity.ok(dailyTrend);
    }
}
