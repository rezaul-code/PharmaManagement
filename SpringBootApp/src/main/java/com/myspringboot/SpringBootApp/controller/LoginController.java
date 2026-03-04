package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    // ─── Show login form ──────────────────────────────────────────────

    @GetMapping("/login")
    public String showLogin(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        return "user_auth/user_login";
    }

    // ─── Handle login submission ──────────────────────────────────────

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("identifier") String identifier,  // email OR phone
            @RequestParam("password")   String password,
            HttpSession session,
            Model model) {

        // Try email first, then phone
        User user = userRepository.findByEmail(identifier);
        if (user == null) {
            user = userRepository.findByPhone(identifier);
        }

        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("error", "Invalid email / phone or password. Please try again.");
            return "user_auth/user_login";
        }

        session.setAttribute("loggedInUser", user);
        session.setAttribute("userId", user.getId());
        return "redirect:/dashboard";
    }

    // ─── Logout ──────────────────────────────────────────────────────

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ─── Root redirect ────────────────────────────────────────────────

    @GetMapping("/")
    public String root(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        return "index";
    }
}