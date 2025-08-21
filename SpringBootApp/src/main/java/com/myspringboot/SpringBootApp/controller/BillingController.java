package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.repo.BillingRepository;
import com.myspringboot.SpringBootApp.repo.MedicineRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;
    private final MedicineRepository medicineRepository;
    private final BillingRepository billingRepository;

    public BillingController(BillingService billingService,
                             MedicineRepository medicineRepository,
                             BillingRepository billingRepository) {
        this.billingService = billingService;
        this.medicineRepository = medicineRepository;
        this.billingRepository = billingRepository;
    }

    // Show new bill form
    @GetMapping("/new")
    public String newBill(Model model) {
        model.addAttribute("bill", new Billing()); // empty bill with no items yet
        model.addAttribute("medicines", medicineRepository.findAll());
        return "pages/billing_new";
    }

    // Save bill with multiple items
    @PostMapping("/save")
    public String saveBill(@ModelAttribute Billing bill,
                           @RequestParam(value = "customerName", required = false) String customerName,
                           Model model) {
        try {
            bill.setCustomerName(customerName);
            Billing saved = billingService.createBillWithItems(bill);
            return "redirect:/billing/" + saved.getId();
        } catch (Exception e) {
            model.addAttribute("bill", bill);
            model.addAttribute("medicines", medicineRepository.findAll());
            model.addAttribute("error", e.getMessage());
            return "pages/billing_new";
        }
    }

    // Show all bills
    @GetMapping
    public String listBills(Model model) {
        model.addAttribute("bills", billingRepository.findAll());
        return "pages/billing_list";
    }

    // View bill details
    @GetMapping("/{id}")
    public String viewBill(@PathVariable Long id, Model model) {
        Billing bill = billingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + id));
        model.addAttribute("bill", bill);
        return "pages/billing_view";
    }
}
