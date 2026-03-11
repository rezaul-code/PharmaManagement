// src/main/java/com/myspringboot/SpringBootApp/controller/BillingController.java
package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.dto.CreditPaymentForm;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
import com.myspringboot.SpringBootApp.model.PaymentType;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/billing")
public class BillingController {

    @Autowired private BillingService        billingService;
    @Autowired private MedicineService       medicineService;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    // ── Guard ──────────────────────────────────────────────────────────

    private User requireLoggedIn(HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null && ra != null)
            ra.addFlashAttribute("error", "Please log in to access billing.");
        return user;
    }

    // ── New bill ───────────────────────────────────────────────────────

    @GetMapping("/new")
    public String newBillForm(HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        model.addAttribute("billingForm",  new BillingForm());
        model.addAttribute("paymentTypes", PaymentType.values());
        model.addAttribute("currentUser",  user);
        return "pages/billing_new";
    }

    // ── Create bill ────────────────────────────────────────────────────

    @PostMapping("/create")
    public String createBill(
            @Valid @ModelAttribute("billingForm") BillingForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes ra) {

        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Please correct the highlighted fields.");
            model.addAttribute("billingForm",  form);
            model.addAttribute("paymentTypes", PaymentType.values());
            model.addAttribute("currentUser",  user);
            return "pages/billing_new";
        }

        try {
            Billing billing = billingService.createBill(form, user);
            ra.addFlashAttribute("success", "Bill " + billing.getBillNumber() + " created successfully.");
            return "redirect:/billing/view/" + billing.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create bill: " + e.getMessage());
            model.addAttribute("billingForm",  form);
            model.addAttribute("paymentTypes", PaymentType.values());
            model.addAttribute("currentUser",  user);
            return "pages/billing_new";
        }
    }

    // ── View single bill ───────────────────────────────────────────────

    @GetMapping("/view/{id}")
    public String viewBill(@PathVariable Long id,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        Optional<Billing> billing = billingService.getBillById(id);
        if (billing.isEmpty()) return "redirect:/billing/list";

        model.addAttribute("billing",            billing.get());
        model.addAttribute("currentUser",        user);
        model.addAttribute("pharmacy",           tenantPharmacyService.getCurrentPharmacy());
        model.addAttribute("creditPaymentForm",  new CreditPaymentForm());
        model.addAttribute("paymentTypes",       PaymentType.values());
        return "pages/billing_view";
    }

    // ── List all bills ─────────────────────────────────────────────────

    @GetMapping("/list")
    public String listBills(HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        model.addAttribute("bills",       billingService.getAllBills());
        model.addAttribute("currentUser", user);
        return "pages/billing_list";
    }

    // ── Credit bills list ──────────────────────────────────────────────

    @GetMapping("/credit")
    public String creditBills(HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        model.addAttribute("bills",         billingService.getPendingCreditBills());
        model.addAttribute("creditSummary", billingService.getCreditSummary());
        model.addAttribute("currentUser",   user);
        return "pages/billing_credit";
    }

    // ── Record credit payment (POST) ───────────────────────────────────

    @PostMapping("/credit/{id}/pay")
    public String recordCreditPayment(
            @PathVariable Long id,
            @Valid @ModelAttribute("creditPaymentForm") CreditPaymentForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes ra) {

        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            ra.addFlashAttribute("error", "Invalid payment amount.");
            return "redirect:/billing/view/" + id;
        }

        try {
            billingService.recordCreditPayment(id, form, user);
            ra.addFlashAttribute("success", "Payment recorded successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Payment failed: " + e.getMessage());
        }
        return "redirect:/billing/view/" + id;
    }

    // ── AJAX: medicine search ──────────────────────────────────────────

    @GetMapping("/medicine/search")
    @ResponseBody
    public List<Medicine> searchMedicine(@RequestParam("q") String query) {
        return medicineService.searchByName(query);
    }

    @GetMapping("/medicine/{id}")
    @ResponseBody
    public ResponseEntity<Medicine> getMedicineById(@PathVariable Long id) {
        return medicineService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}