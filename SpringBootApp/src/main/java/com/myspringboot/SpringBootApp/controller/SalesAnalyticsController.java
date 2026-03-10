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
     * @param metric "quantity" | "revenue" | "profit"  (default: quantity)
     * @param period "7d" | "30d" | "90d" | "1y"        (default: 30d)
     */
    @GetMapping("/sales-analytics")
    public String salesAnalytics(
            @RequestParam(defaultValue = "quantity") String metric,
            @RequestParam(defaultValue = "30d")      String period,
            HttpSession session,
            Model model) {

        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";

        List<SalesAnalyticsResult> topMedicines =
                salesAnalyticsService.getTopMedicines(metric, period);
        Map<String, BigDecimal> summary =
                salesAnalyticsService.getSummary(period);
        Map<String, BigDecimal> dailyTrend =
                salesAnalyticsService.getDailyRevenueTrend(period);

        // JSON-safe arrays for bar chart
        StringBuilder chartLabels = new StringBuilder("[");
        StringBuilder chartValues = new StringBuilder("[");
        for (int i = 0; i < topMedicines.size(); i++) {
            SalesAnalyticsResult r = topMedicines.get(i);
            String name = r.getMedicineName()
                           .replace("\"", "\\\"").replace("'", "\\'");
            chartLabels.append("\"").append(name).append("\"");
            BigDecimal val = switch (metric) {
                case "revenue" -> r.getTotalRevenue();
                case "profit"  -> r.getTotalProfit();
                default        -> BigDecimal.valueOf(r.getTotalQuantity());
            };
            chartValues.append(val.toPlainString());
            if (i < topMedicines.size() - 1) { chartLabels.append(","); chartValues.append(","); }
        }
        chartLabels.append("]"); chartValues.append("]");

        // JSON-safe arrays for daily trend line chart
        StringBuilder trendLabels = new StringBuilder("[");
        StringBuilder trendValues = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, BigDecimal> e : dailyTrend.entrySet()) {
            if (!first) { trendLabels.append(","); trendValues.append(","); }
            trendLabels.append("\"").append(e.getKey()).append("\"");
            trendValues.append(e.getValue().toPlainString());
            first = false;
        }
        trendLabels.append("]"); trendValues.append("]");

        model.addAttribute("topMedicines", topMedicines);
        model.addAttribute("summary",      summary);
        model.addAttribute("metric",       metric);
        model.addAttribute("period",       period);
        model.addAttribute("chartLabels",  chartLabels.toString());
        model.addAttribute("chartValues",  chartValues.toString());
        model.addAttribute("trendLabels",  trendLabels.toString());
        model.addAttribute("trendValues",  trendValues.toString());
        model.addAttribute("activePage",   "sales-analytics");

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
    // 3. GET /monthly-trends — Monthly revenue + profit dual-line chart
    // ════════════════════════════════════════════════════════════════════════

    /**
     * @param monthlyPeriod 6 | 12  (default: 6)
     */
    @GetMapping("/monthly-trends")
    public String monthlyTrends(
            @RequestParam(defaultValue = "6") int monthlyPeriod,
            HttpSession session,
            Model model) {

        if (session.getAttribute("loggedInUser") == null) return "redirect:/login";

        int safeMonths = (monthlyPeriod == 12) ? 12 : 6;

        List<MonthlyTrendResult> monthlyTrend =
                salesAnalyticsService.getMonthlyTrend(safeMonths);

        StringBuilder mLabels  = new StringBuilder("[");
        StringBuilder mRevenue = new StringBuilder("[");
        StringBuilder mProfit  = new StringBuilder("[");
        StringBuilder mUnits   = new StringBuilder("[");

        for (int i = 0; i < monthlyTrend.size(); i++) {
            MonthlyTrendResult r = monthlyTrend.get(i);
            mLabels .append("\"").append(r.getMonthLabel()).append("\"");
            mRevenue.append(r.getRevenue().toPlainString());
            mProfit .append(r.getProfit() .toPlainString());
            mUnits  .append(r.getUnits());
            if (i < monthlyTrend.size() - 1) {
                mLabels.append(","); mRevenue.append(",");
                mProfit.append(","); mUnits  .append(",");
            }
        }
        mLabels.append("]"); mRevenue.append("]");
        mProfit.append("]"); mUnits  .append("]");

        model.addAttribute("monthlyTrend",   monthlyTrend);
        model.addAttribute("monthlyPeriod",  safeMonths);
        model.addAttribute("monthlyLabels",  mLabels .toString());
        model.addAttribute("monthlyRevenue", mRevenue.toString());
        model.addAttribute("monthlyProfit",  mProfit .toString());
        model.addAttribute("monthlyUnits",   mUnits  .toString());
        model.addAttribute("activePage",     "monthly-trends");

        return "pages/monthly-trends";
    }
}