package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.UserService;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SignUpController {

    @Autowired
    private UserService userService;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @GetMapping("/signup")
    public String showSignup(Model model) {
        model.addAttribute("user", new User());
        return "user_auth/user_signup";
    }

    @PostMapping("/signup")
    public String handleSignup(
            @ModelAttribute("user") User user,
            @RequestParam("pharmacyName") String pharmacyName,
            @RequestParam(value = "pharmacyAddress", required = false) String pharmacyAddress,
            @RequestParam(value = "pharmacyPhone", required = false) String pharmacyPhone,
            @RequestParam(value = "pharmacyEmail", required = false) String pharmacyEmail,
            @RequestParam(value = "pharmacyLicense", required = false) String pharmacyLicense,
            Model model) {

        // ── Validate user fields ──────────────────────────────────────
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            model.addAttribute("error", "Full name is required.");
            return "user_auth/user_signup";
        }

        boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isBlank();

        if (!hasEmail && !hasPhone) {
            model.addAttribute("error", "Please provide at least an email or phone number.");
            return "user_auth/user_signup";
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            model.addAttribute("error", "Password is required.");
            return "user_auth/user_signup";
        }

        // ── Validate pharmacy name ────────────────────────────────────
        if (pharmacyName == null || pharmacyName.isBlank()) {
            model.addAttribute("error", "Pharmacy name is required.");
            return "user_auth/user_signup";
        }

        // ── Step 1: Create a NEW pharmacy for this owner ─────────────
        // Every signup creates their own isolated pharmacy — no sharing.
        Pharmacy pharmacy = new Pharmacy();
        pharmacy.setName(pharmacyName.trim());
        pharmacy.setAddress(pharmacyAddress);
        pharmacy.setPhone(pharmacyPhone);
        pharmacy.setEmail(pharmacyEmail);
        pharmacy.setLicenseNumber(pharmacyLicense);
        Pharmacy savedPharmacy = pharmacyRepository.save(pharmacy);

        // ── Step 2: Save user linked to the new pharmacy ─────────────
        user.setPharmacy(savedPharmacy);
        userService.saveUserWithPharmacy(user, savedPharmacy);

        return "redirect:/login?registered=true";
    }
}