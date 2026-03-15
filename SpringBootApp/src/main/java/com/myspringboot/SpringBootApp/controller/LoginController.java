package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.TenantContext;
import com.myspringboot.SpringBootApp.Service.UserService;
import com.myspringboot.SpringBootApp.model.User;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired private UserService userService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

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

        // ── 1. Lookup user by email OR phone ──────────────────────────
        User user = userService.findByIdentifierGlobal(identifier);

        if (user == null) {
            log.warn("LOGIN_FAIL | identifier='{}' | reason=USER_NOT_FOUND", identifier);
            ra.addFlashAttribute("error", "Invalid email/phone or password.");
            return "redirect:/login";
        }

        // ── 2. Password verification (supports legacy plain-text + BCrypt) ──
        if (!verifyPassword(password, user)) {
            log.warn("LOGIN_FAIL | identifier='{}' userId={} | reason=BAD_PASSWORD",
                     identifier, user.getId());
            ra.addFlashAttribute("error", "Invalid email/phone or password.");
            return "redirect:/login";
        }

        // ── 3. Account status check ───────────────────────────────────
        String status = user.getStatus();
        if ("PENDING".equalsIgnoreCase(status)) {
            log.info("LOGIN_FAIL | userId={} | reason=PENDING_ACCOUNT", user.getId());
            ra.addFlashAttribute("error",
                    "Your account is pending approval. Please contact your pharmacy owner.");
            return "redirect:/login";
        }
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            log.warn("LOGIN_FAIL | userId={} | reason=INACTIVE_STATUS status='{}'",
                     user.getId(), status);
            ra.addFlashAttribute("error",
                    "Your account is not active. Please contact your pharmacy owner.");
            return "redirect:/login";
        }

        // ── 4. Success — set session + tenant context ─────────────────
        session.setAttribute("loggedInUser", user);
        session.setAttribute("pharmacyId",   user.getPharmacy().getId());
        TenantContext.setCurrentPharmacyId(user.getPharmacy().getId());

        log.info("LOGIN_OK | userId={} pharmacyId={}", user.getId(), user.getPharmacy().getId());
        return "redirect:/dashboard";
    }

    /**
     * Supports both BCrypt-hashed and legacy plain-text passwords.
     * If a plain-text match succeeds, the password is automatically
     * upgraded to BCrypt in the database (transparent migration).
     */
    private boolean verifyPassword(String rawPassword, User user) {
        String stored = user.getPassword();
        if (stored == null) return false;

        // BCrypt hashes always start with "$2a$", "$2b$", or "$2y$"
        if (stored.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, stored);
        }

        // Legacy plain-text comparison
        if (stored.equals(rawPassword)) {
            // Auto-upgrade to BCrypt
            user.setPassword(passwordEncoder.encode(rawPassword));
            userService.updatePassword(user);
            log.info("PASSWORD_UPGRADED | userId={} | plain-text → BCrypt", user.getId());
            return true;
        }

        return false;
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