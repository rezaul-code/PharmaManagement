package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.BillingService;
import com.myspringboot.SpringBootApp.Service.MedicineService;
import com.myspringboot.SpringBootApp.Service.TenantPharmacyService;
import com.myspringboot.SpringBootApp.dto.BillingForm;
import com.myspringboot.SpringBootApp.model.Billing;
import com.myspringboot.SpringBootApp.model.Medicine;
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

    @Autowired private BillingService billingService;
    @Autowired private MedicineService medicineService;
    @Autowired private TenantPharmacyService tenantPharmacyService;

    // ── Guard helper ──────────────────────────────────────────────────

    /**
     * All three roles (OWNER, PHARMACIST, STAFF) can access billing.
     * This guard simply ensures the user is logged in.
     */
    private User requireLoggedIn(HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null && ra != null) {
            ra.addFlashAttribute("error", "Please log in to access billing.");
        }
        return user;
    }

    // ── New bill form (all roles) ─────────────────────────────────────

    @GetMapping("/new")
    public String newBillForm(HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        model.addAttribute("billingForm", new BillingForm());
        model.addAttribute("currentUser", user);
        return "pages/billing_new";
    }

    // ── Create bill (all roles) ───────────────────────────────────────

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
            model.addAttribute("error",
                    "Please correct the highlighted billing form fields.");
            model.addAttribute("billingForm", form);
            model.addAttribute("currentUser", user);
            return "pages/billing_new";
        }

        try {
            Billing billing = billingService.createBill(form, user);
            return "redirect:/billing/view/" + billing.getId();
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create bill: " + e.getMessage());
            model.addAttribute("billingForm", form);
            model.addAttribute("currentUser", user);
            return "pages/billing_new";
        }
    }

    // ── View single bill (all roles) ──────────────────────────────────

    @GetMapping("/view/{id}")
    public String viewBill(@PathVariable Long id,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        Optional<Billing> billing = billingService.getBillById(id);
        if (billing.isEmpty()) return "redirect:/billing/list";

        model.addAttribute("billing",     billing.get());
        model.addAttribute("currentUser", user);
        model.addAttribute("pharmacy",    tenantPharmacyService.getCurrentPharmacy());
        return "pages/billing_view";
    }

    // ── List all bills (all roles) ────────────────────────────────────

    @GetMapping("/list")
    public String listBills(HttpSession session, Model model, RedirectAttributes ra) {
        User user = requireLoggedIn(session, ra);
        if (user == null) return "redirect:/login";

        model.addAttribute("bills",       billingService.getAllBills());
        model.addAttribute("currentUser", user);
        return "pages/billing_list";
    }

    // ── AJAX: medicine search (all roles) ─────────────────────────────

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