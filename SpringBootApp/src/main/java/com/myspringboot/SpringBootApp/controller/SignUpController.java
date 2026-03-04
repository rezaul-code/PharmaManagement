package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SignUpController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/signup")
    public String showSignup(Model model) {
        model.addAttribute("user", new User());
        return "user_auth/user_signup";
    }

    @PostMapping("/signup")
    public String handleSignup(@ModelAttribute("user") User user, Model model) {

        // Username required
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            model.addAttribute("error", "Full name is required.");
            return "user_auth/user_signup";
        }

        // Must provide email OR phone (at least one)
        boolean hasEmail = user.getEmail() != null && !user.getEmail().isBlank();
        boolean hasPhone = user.getPhone() != null && !user.getPhone().isBlank();

        if (!hasEmail && !hasPhone) {
            model.addAttribute("error", "Please provide at least an email or phone number.");
            return "user_auth/user_signup";
        }

        // Password required
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            model.addAttribute("error", "Password is required.");
            return "user_auth/user_signup";
        }

        // Duplicate email check
        if (hasEmail && userRepository.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "This email is already registered. Please log in.");
            return "user_auth/user_signup";
        }

        // Duplicate phone check
        if (hasPhone && userRepository.existsByPhone(user.getPhone())) {
            model.addAttribute("error", "This phone number is already registered. Please log in.");
            return "user_auth/user_signup";
        }

        userRepository.save(user);
        return "redirect:/login?registered=true";
    }
}