package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.TenantContext;
import com.myspringboot.SpringBootApp.Service.UserService;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLogin(HttpSession session, Model model,
                            @RequestParam(value = "registered", required = false) String registered,
                            @RequestParam(value = "invited",    required = false) String invited) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        if (registered != null) {
            model.addAttribute("success", "Registration successful! Please log in.");
        }
        if (invited != null) {
            model.addAttribute("success", "Account created! You can now log in.");
        }
        return "user_auth/user_login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("identifier") String identifier,
            @RequestParam("password")   String password,
            HttpSession session,
            RedirectAttributes ra) {

        User user = userService.findByIdentifierGlobal(identifier);

        // ── User not found ────────────────────────────────────────────
        if (user == null) {
            ra.addFlashAttribute("error", "Invalid email/phone or password.");
            return "redirect:/login";
        }

        // ── Wrong password ────────────────────────────────────────────
        if (!user.getPassword().equals(password)) {
            ra.addFlashAttribute("error", "Invalid email/phone or password.");
            return "redirect:/login";
        }

        // ── PENDING account — block login ─────────────────────────────
        if ("PENDING".equals(user.getStatus())) {
            ra.addFlashAttribute("error",
                    "Your account is pending approval. Please contact your pharmacy owner.");
            return "redirect:/login";
        }

        // ── Success ───────────────────────────────────────────────────
        session.setAttribute("loggedInUser", user);
        session.setAttribute("pharmacyId",   user.getPharmacy().getId());
        TenantContext.setCurrentPharmacyId(user.getPharmacy().getId()); // ← fixed

        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        TenantContext.clear();
        return "redirect:/login";
    }

    @GetMapping("/")
    public String root(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        return "index";
    }
}