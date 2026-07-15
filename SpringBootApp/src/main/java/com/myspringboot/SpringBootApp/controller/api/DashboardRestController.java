package com.myspringboot.SpringBootApp.controller.api;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.SalesAnalyticsService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.dto.CreditSummaryDto;
import com.myspringboot.SpringBootApp.dto.MonthlyTrendResult;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardRestController {

    @Autowired private BillingService billingService;
    @Autowired private MedicineService medicineService;
    @Autowired private SalesAnalyticsService salesAnalyticsService;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    @Autowired private BillingRepository billingRepository;
    @Autowired private MedicineRepository medicineRepository;

    // ── GET /api/dashboard/summary ───────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pharmacy pharmacy = tenantPharmacyService.getCurrentPharmacy();

        long totalMedicines = medicineService.getTotalCount();
        long lowStockCount = medicineService.getLowStockCount();
        long expiringCount = medicineService.getExpiringWithin30DaysCount();
        BigDecimal todaySales = billingService.getTodaySales();

        // Count of today's bills
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime endOfToday = LocalDate.now().atTime(23, 59, 59);
        List<Billing> todayBills = billingRepository
                .findByPharmacyIdAndCreatedAtBetweenOrderByCreatedAtDesc(pharmacyId, startOfToday, endOfToday);
        long todayBillsCount = todayBills.size();

        // Total revenue (using early date as all-time start)
        BigDecimal totalRevenue = billingRepository
                .sumGrandTotalBetween(pharmacyId, LocalDateTime.of(2000, 1, 1, 0, 0), LocalDate.now().plusDays(1).atStartOfDay());
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Credit summary values
        CreditSummaryDto creditSummary = billingService.getCreditSummary();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalMedicines", totalMedicines);
        summary.put("lowStockCount", lowStockCount);
        summary.put("expiringMedicinesCount", expiringCount);
        summary.put("todaySales", todaySales);
        summary.put("todayBillsCount", todayBillsCount);
        summary.put("totalRevenue", totalRevenue);
        summary.put("totalCredit", creditSummary.getTotalCreditIssued());
        summary.put("pendingCredit", creditSummary.getPendingBalance());
        summary.put("pharmacy", pharmacy);

        return ResponseEntity.ok(summary);
    }

    // ── GET /api/dashboard/recent-sales ──────────────────────────────────
    @GetMapping("/recent-sales")
    public ResponseEntity<List<Billing>> getRecentSales() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        List<Billing> recentSales = billingRepository
                .findTop5ByPharmacyIdOrderByCreatedAtDesc(pharmacyId);
        return ResponseEntity.ok(recentSales);
    }

    // ── GET /api/dashboard/recent-medicines ──────────────────────────────
    @GetMapping("/recent-medicines")
    public ResponseEntity<List<Medicine>> getRecentMedicines() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        Pageable limit = PageRequest.of(0, 5, Sort.by("id").descending());
        Page<Medicine> page = medicineRepository.findByPharmacyId(pharmacyId, limit);
        return ResponseEntity.ok(page.getContent());
    }

    // ── GET /api/dashboard/charts/sales ──────────────────────────────────
    @GetMapping("/charts/sales")
    public ResponseEntity<?> getSalesChart(@RequestParam(defaultValue = "7d") String period) {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        return switch (period) {
            case "30d" -> ResponseEntity.ok(buildDailySalesData(pharmacyId, 30));
            case "90d" -> ResponseEntity.ok(buildWeeklySalesData(pharmacyId));
            case "1y"  -> ResponseEntity.ok(buildMonthlySalesData(pharmacyId));
            default    -> ResponseEntity.ok(buildDailySalesData(pharmacyId, 7)); // "7d"
        };
    }

    // ── GET /api/dashboard/charts/inventory ──────────────────────────────
    @GetMapping("/charts/inventory")
    public ResponseEntity<Map<String, Long>> getInventoryChart() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();
        long outOfStock = medicineRepository.countByPharmacyIdAndStockQuantityEquals(pharmacyId, 0);
        long critical = medicineRepository.countByPharmacyIdAndStockQuantityBetween(pharmacyId, 1, 3);
        long low = medicineRepository.countByPharmacyIdAndStockQuantityBetween(pharmacyId, 4, 10);
        long healthy = medicineRepository.countByPharmacyIdAndStockQuantityGreaterThan(pharmacyId, 10);

        Map<String, Long> data = new LinkedHashMap<>();
        data.put("outOfStock", outOfStock);
        data.put("critical", critical);
        data.put("low", low);
        data.put("healthy", healthy);

        return ResponseEntity.ok(data);
    }

    // ── GET /api/dashboard/charts/revenue ────────────────────────────────
    @GetMapping("/charts/revenue")
    public ResponseEntity<List<MonthlyTrendResult>> getRevenueChart(@RequestParam(defaultValue = "6m") String period) {
        List<MonthlyTrendResult> trendData;
        LocalDate today = LocalDate.now();

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
        return ResponseEntity.ok(trendData);
    }

    // ── GET /api/dashboard/notifications ─────────────────────────────────
    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> getNotifications() {
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacyId();

        List<Medicine> lowStock = medicineService.getLowStockMedicines();
        List<Medicine> expiring = medicineService.getExpiringWithin30Days();

        Page<Billing> pendingCreditsPage = billingService.getPendingCreditBills(PageRequest.of(0, 100));
        List<Billing> pendingCredits = pendingCreditsPage.getContent();

        Map<String, Object> notifications = new HashMap<>();
        notifications.put("lowStockAlerts", lowStock);
        notifications.put("expiringMedicines", expiring);
        notifications.put("pendingCredits", pendingCredits);

        return ResponseEntity.ok(notifications);
    }

    // ── Chart Builders ───────────────────────────────────────────────────

    private List<Map<String, Object>> buildDailySalesData(Long pharmacyId, int days) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(days - 1).atStartOfDay();

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        for (Object[] row : billingRepository.findDailyRevenueBetween(pharmacyId, start, end)) {
            LocalDate day = toLocalDate(row[0]);
            revenueByDate.put(day, toBigDecimal(row[1]));
        }

        DateTimeFormatter fmt = (days <= 7)
                ? DateTimeFormatter.ofPattern("EEE d")
                : DateTimeFormatter.ofPattern("d MMM");

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("label", day.format(fmt));
            pt.put("revenue", revenueByDate.getOrDefault(day, BigDecimal.ZERO));
            result.add(pt);
        }
        return result;
    }

    private List<Map<String, Object>> buildWeeklySalesData(Long pharmacyId) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusDays(90).atStartOfDay();

        Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();
        for (Object[] row : billingRepository.findDailyRevenueBetween(pharmacyId, start, end)) {
            revenueByDate.put(toLocalDate(row[0]), toBigDecimal(row[1]));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate monday = LocalDate.now().minusWeeks(12);
        monday = monday.minusDays(monday.getDayOfWeek().getValue() - 1);

        for (int w = 0; w < 13; w++) {
            LocalDate weekStart = monday.plusWeeks(w);
            LocalDate weekEnd = weekStart.plusDays(6);

            BigDecimal total = BigDecimal.ZERO;
            for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
                total = total.add(revenueByDate.getOrDefault(d, BigDecimal.ZERO));
            }

            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("label", weekStart.format(fmt));
            pt.put("revenue", total);
            result.add(pt);
        }
        return result;
    }

    private List<Map<String, Object>> buildMonthlySalesData(Long pharmacyId) {
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime start = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (Object[] row : billingRepository.findMonthlyRevenue(pharmacyId, start, end)) {
            int yr = ((Number) row[0]).intValue();
            int mo = ((Number) row[1]).intValue();
            String key = yr + "-" + String.format("%02d", mo);
            revenueByMonth.put(key, toBigDecimal(row[2]));
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yy");
        List<Map<String, Object>> result = new ArrayList<>();

        YearMonth cursor = YearMonth.now().minusMonths(11);
        for (int m = 0; m < 12; m++) {
            YearMonth ym = cursor.plusMonths(m);
            String key = ym.getYear() + "-" + String.format("%02d", ym.getMonthValue());

            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("label", ym.atDay(1).format(fmt));
            pt.put("revenue", revenueByMonth.getOrDefault(key, BigDecimal.ZERO));
            result.add(pt);
        }
        return result;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof java.sql.Date) return ((java.sql.Date) o).toLocalDate();
        if (o instanceof LocalDate) return (LocalDate) o;
        return LocalDate.parse(o.toString());
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }
}
