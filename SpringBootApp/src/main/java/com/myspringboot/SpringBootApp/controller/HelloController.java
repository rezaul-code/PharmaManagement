package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private TenantPharmacyService tenantPharmacyService;   // ← ADD THIS

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // ── Pharmacy info (tenant-scoped) ────────────────────────────
        model.addAttribute("pharmacy", tenantPharmacyService.getCurrentPharmacy());

        // ── Stat cards ───────────────────────────────────────────────
        model.addAttribute("totalMedicines", medicineService.getTotalCount());
        model.addAttribute("lowStockCount",  medicineService.getLowStockCount());
        model.addAttribute("totalBills",     billingService.getTotalBillCount());
        model.addAttribute("todaySales",     billingService.getTodaySales());

        // ── Tables ───────────────────────────────────────────────────
        model.addAttribute("lowStockMeds",   medicineService.getLowStockMedicines());
        model.addAttribute("recentBills",
                billingService.getAllBills().stream().limit(5).toList());

        return "pages/dashboard";
    }
}