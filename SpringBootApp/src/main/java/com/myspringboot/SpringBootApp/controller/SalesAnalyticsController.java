package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.SalesAnalyticsService;
import com.myspringboot.SpringBootApp.dto.SalesAnalyticsResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
public class SalesAnalyticsController {

    @Autowired
    private SalesAnalyticsService salesAnalyticsService;

    @Autowired
    private MedicineService medicineService;   // ← for inventory analytics cards

    /**
     * GET /sales-analytics
     *
     * @param metric "quantity" | "revenue" | "profit"   (default: quantity)
     * @param period "7d" | "30d" | "90d" | "1y"         (default: 30d)
     */
    @GetMapping("/sales-analytics")
    public String salesAnalytics(
            @RequestParam(defaultValue = "quantity") String metric,
            @RequestParam(defaultValue = "30d")      String period,
            HttpSession session,
            Model model) {

        // ── Auth guard ───────────────────────────────────────────────────
        Object user = session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        // ── Sales analytics data (charts + top-10 table) ─────────────────
        List<SalesAnalyticsResult> topMedicines =
                salesAnalyticsService.getTopMedicines(metric, period);

        Map<String, BigDecimal> summary =
                salesAnalyticsService.getSummary(period);

        Map<String, BigDecimal> dailyTrend =
                salesAnalyticsService.getDailyRevenueTrend(period);

        // ── Inventory analytics cards (moved from All Medicines page) ─────
        BigDecimal totalCostValue    = medicineService.getTotalInventoryCostValue();
        BigDecimal totalSellingValue = medicineService.getTotalInventorySellingValue();
        BigDecimal potentialProfit   = medicineService.getPotentialInventoryProfit();
        long negativeMarginCount     = medicineService.getNegativeMarginCount();

        // ── Chart data as JSON-safe strings for Chart.js ──────────────────
        StringBuilder chartLabels = new StringBuilder("[");
        StringBuilder chartValues = new StringBuilder("[");

        for (int i = 0; i < topMedicines.size(); i++) {
            SalesAnalyticsResult r = topMedicines.get(i);
            String name = r.getMedicineName()
                           .replace("\"", "\\\"")
                           .replace("'",  "\\'");

            chartLabels.append("\"").append(name).append("\"");

            BigDecimal val = switch (metric) {
                case "revenue" -> r.getTotalRevenue();
                case "profit"  -> r.getTotalProfit();
                default        -> BigDecimal.valueOf(r.getTotalQuantity());
            };
            chartValues.append(val.toPlainString());

            if (i < topMedicines.size() - 1) {
                chartLabels.append(",");
                chartValues.append(",");
            }
        }
        chartLabels.append("]");
        chartValues.append("]");

        // Trend chart data
        StringBuilder trendLabels = new StringBuilder("[");
        StringBuilder trendValues = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, BigDecimal> e : dailyTrend.entrySet()) {
            if (!first) { trendLabels.append(","); trendValues.append(","); }
            trendLabels.append("\"").append(e.getKey()).append("\"");
            trendValues.append(e.getValue().toPlainString());
            first = false;
        }
        trendLabels.append("]");
        trendValues.append("]");

        // ── Model ─────────────────────────────────────────────────────────
        // Sales analytics
        model.addAttribute("topMedicines",  topMedicines);
        model.addAttribute("summary",       summary);
        model.addAttribute("metric",        metric);
        model.addAttribute("period",        period);
        model.addAttribute("chartLabels",   chartLabels.toString());
        model.addAttribute("chartValues",   chartValues.toString());
        model.addAttribute("trendLabels",   trendLabels.toString());
        model.addAttribute("trendValues",   trendValues.toString());

        // Inventory analytics (relocated from All Medicines)
        model.addAttribute("totalCostValue",      totalCostValue);
        model.addAttribute("totalSellingValue",   totalSellingValue);
        model.addAttribute("potentialProfit",     potentialProfit);
        model.addAttribute("negativeMarginCount", negativeMarginCount);

        model.addAttribute("activePage", "sales-analytics");

        return "pages/sales-analytics";
    }
}