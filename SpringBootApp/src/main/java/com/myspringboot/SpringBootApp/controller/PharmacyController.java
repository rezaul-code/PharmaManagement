package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.TenantContext;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pharmacy")
public class PharmacyController {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    // ── GET /pharmacy/settings ────────────────────────────────────────
    @GetMapping("/settings")
    public String settingsPage(HttpSession session, Model model) {

        // Guard: OWNER only
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            return "redirect:/dashboard";
        }

        Long pharmacyId = TenantContext.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pharmacy not found for id: " + pharmacyId));

        model.addAttribute("pharmacy", pharmacy);
        return "pages/pharmacy_settings";
    }

    // ── POST /pharmacy/settings ───────────────────────────────────────
    @PostMapping("/settings")
    public String saveSettings(@ModelAttribute("pharmacy") Pharmacy formData,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        // Guard: OWNER only
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !loggedInUser.isOwner()) {
            return "redirect:/dashboard";
        }

        Long pharmacyId = TenantContext.getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pharmacy not found for id: " + pharmacyId));

        // Apply updates from form
        pharmacy.setName(formData.getName());
        pharmacy.setGstNumber(formData.getGstNumber());
        pharmacy.setPhone(formData.getPhone());
        pharmacy.setAddress(formData.getAddress());
        pharmacy.setInvoiceFooter(formData.getInvoiceFooter());

        pharmacyRepository.save(pharmacy);

        redirectAttributes.addFlashAttribute("successMessage",
                "Pharmacy settings saved successfully!");

        return "redirect:/pharmacy/settings";
    }
}