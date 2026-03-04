package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
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

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Stats cards
        model.addAttribute("totalMedicines",  medicineService.getTotalCount());
        model.addAttribute("lowStockCount",   medicineService.getLowStockCount());
        model.addAttribute("totalBills",      billingService.getTotalBillCount());
        model.addAttribute("todaySales",      billingService.getTodaySales());

        // Low-stock table
        model.addAttribute("lowStockMeds",    medicineService.getLowStockMedicines());

        // Recent bills (first 5 from already-sorted list)
        model.addAttribute("recentBills",
            billingService.getAllBills().stream().limit(5).toList());

        return "pages/dashboard";
    }
}