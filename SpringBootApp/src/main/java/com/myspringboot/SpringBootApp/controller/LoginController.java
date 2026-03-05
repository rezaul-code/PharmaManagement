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
import java.util.Objects;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String showLogin(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        return "user_auth/user_login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("identifier") String identifier,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        // ── Search across ALL pharmacies by email or phone ────────────
        // We cannot scope by pharmacyId here because the session has none yet.
        // findByIdentifierGlobal() searches the users table without a pharmacy filter.
        User user = userService.findByIdentifierGlobal(identifier);

        if (user == null || !Objects.equals(user.getPassword(), password)) {
            model.addAttribute("error", "Invalid email / phone or password. Please try again.");
            return "user_auth/user_login";
        }

        // ── Resolve pharmacyId from the user's own pharmacy link ──────
        Long pharmacyId = TenantContext.DEFAULT_PHARMACY_ID;
        if (user.getPharmacy() != null && user.getPharmacy().getId() != null) {
            pharmacyId = user.getPharmacy().getId();
        }

        // ── Store in session ──────────────────────────────────────────
        session.setAttribute("loggedInUser", user);
        session.setAttribute("userId", user.getId());
        session.setAttribute("pharmacyId", pharmacyId);

        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
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