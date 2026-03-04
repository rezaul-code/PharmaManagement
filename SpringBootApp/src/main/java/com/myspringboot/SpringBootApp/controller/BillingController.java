package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.BillingItemForm;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/billing")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private MedicineService medicineService;

    // ─── New Bill Form ────────────────────────────────────────────────

    @GetMapping("/new")
    public String newBillForm(Model model) {
        BillingForm form = new BillingForm();
        // Pre-populate with one empty row
        List<BillingItemForm> items = new ArrayList<>();
        items.add(new BillingItemForm());
        form.setItems(items);

        model.addAttribute("billingForm", form);
        model.addAttribute("medicines", medicineService.getAll());
        return "pages/billing_new";
    }

    // ─── Create Bill ──────────────────────────────────────────────────

    @PostMapping("/create")
    public String createBill(
            @ModelAttribute("billingForm") BillingForm form,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        try {
            Billing billing = billingService.createBill(form, user);
            return "redirect:/billing/view/" + billing.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create bill: " + e.getMessage());
            model.addAttribute("billingForm", form);
            model.addAttribute("medicines", medicineService.getAll());
            return "pages/billing_new";
        }
    }

    // ─── View Single Bill ─────────────────────────────────────────────

    @GetMapping("/view/{id}")
    public String viewBill(@PathVariable Long id, Model model) {
        Optional<Billing> billing = billingService.getBillById(id);
        if (billing.isEmpty()) {
            return "redirect:/billing/list";
        }
        model.addAttribute("billing", billing.get());
        return "pages/billing_view";
    }

    // ─── Bill List ────────────────────────────────────────────────────

    @GetMapping("/list")
    public String listBills(Model model) {
        model.addAttribute("bills", billingService.getAllBills());
        return "pages/billing_list";
    }

    // ─── Medicine Autocomplete API ────────────────────────────────────

    @GetMapping("/medicine/search")
    @ResponseBody
    public List<Medicine> searchMedicine(@RequestParam("q") String query) {
        return medicineService.searchByName(query);
    }

    // ─── Medicine Detail by ID (for auto-fill price / GST) ───────────

    @GetMapping("/medicine/{id}")
    @ResponseBody
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return medicineService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}