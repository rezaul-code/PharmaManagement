// ─────────────────────────────────────────────────────────────────────────────
// HelloController.java — full updated file
// ─────────────────────────────────────────────────────────────────────────────
package com.myspringboot.SpringBootApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class HelloController {

    @Autowired private MedicineService       medicineService;
    @Autowired private BillingService        billingService;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    // Inject repositories directly for the chart queries
    @Autowired private BillingRepository     billingRepository;
    @Autowired private MedicineRepository    medicineRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {

        // ── Resolve current pharmacy ID ──────────────────────────────────
        Long pharmacyId = tenantPharmacyService.getCurrentPharmacy() != null
                ? tenantPharmacyService.getCurrentPharmacy().getId()
                : null;

        // ── Pharmacy info ────────────────────────────────────────────────
        model.addAttribute("pharmacy", tenantPharmacyService.getCurrentPharmacy());

        // ── Stat cards ───────────────────────────────────────────────────
        model.addAttribute("totalMedicines", medicineService.getTotalCount());
        model.addAttribute("lowStockCount",  medicineService.getLowStockCount());
        model.addAttribute("expiringCount",  medicineService.getExpiringWithin30DaysCount());
        model.addAttribute("todaySales",     billingService.getTodaySales());

        // ── (kept for any other use, tables removed from template) ───────
        model.addAttribute("lowStockMeds",  medicineService.getLowStockMedicines());
        model.addAttribute("recentBills",
                billingService.getAllBills().stream().limit(5).toList());

        // ── Chart data ───────────────────────────────────────────────────
        model.addAttribute("salesChartJson", buildSalesChartJson(pharmacyId));
        model.addAttribute("stockChartJson", buildStockChartJson(pharmacyId));

        return "pages/dashboard";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHART HELPER — Sales last 7 days
    //
    // Returns a JSON array: [ {"label":"Mon 9","revenue":1234.50}, ... ]
    // One entry per day, always 7 entries (revenue = 0 if no sales that day).
    // ─────────────────────────────────────────────────────────────────────────
    private String buildSalesChartJson(Long pharmacyId) {
        try {
            // Build a map of date → revenue from the DB
            Map<LocalDate, BigDecimal> revenueByDate = new LinkedHashMap<>();

            if (pharmacyId != null) {
                LocalDateTime end   = LocalDate.now().plusDays(1).atStartOfDay();
                LocalDateTime start = LocalDate.now().minusDays(6).atStartOfDay();

                List<Object[]> rows = billingRepository.findDailyRevenueBetween(pharmacyId, start, end);
                for (Object[] row : rows) {
                    // row[0] = java.sql.Date or LocalDate depending on dialect
                    LocalDate day = (row[0] instanceof java.sql.Date)
                            ? ((java.sql.Date) row[0]).toLocalDate()
                            : (LocalDate) row[0];
                    BigDecimal rev = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    revenueByDate.put(day, rev);
                }
            }

            // Build a full 7-day list, filling zeros for missing days
            DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("EEE d"); // "Mon 9"
            List<Map<String, Object>> result = new ArrayList<>();

            for (int i = 6; i >= 0; i--) {
                LocalDate day = LocalDate.now().minusDays(i);
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("label",   day.format(labelFmt));
                point.put("revenue", revenueByDate.getOrDefault(day, BigDecimal.ZERO));
                result.add(point);
            }

            return MAPPER.writeValueAsString(result);

        } catch (Exception e) {
            return "[]";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CHART HELPER — Stock health breakdown
    //
    // Returns JSON: {"outOfStock":5,"critical":3,"low":12,"healthy":80}
    //
    // Buckets:
    //   outOfStock : stockQuantity == 0
    //   critical   : 1–3
    //   low        : 4–10
    //   healthy    : > 10
    // ─────────────────────────────────────────────────────────────────────────
    private String buildStockChartJson(Long pharmacyId) {
        try {
            long outOfStock = 0, critical = 0, low = 0, healthy = 0;

            if (pharmacyId != null) {
                List<Medicine> all = medicineRepository.findByPharmacyId(pharmacyId);
                for (Medicine m : all) {
                    int qty = m.getStockQuantity();
                    if      (qty == 0)          outOfStock++;
                    else if (qty <= 3)           critical++;
                    else if (qty <= 10)          low++;
                    else                         healthy++;
                }
            }

            Map<String, Long> data = new LinkedHashMap<>();
            data.put("outOfStock", outOfStock);
            data.put("critical",   critical);
            data.put("low",        low);
            data.put("healthy",    healthy);

            return MAPPER.writeValueAsString(data);

        } catch (Exception e) {
            return "{\"outOfStock\":0,\"critical\":0,\"low\":0,\"healthy\":0}";
        }
    }
}