package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.StaffService;
import com.myspringboot.SpringBootApp.model.Role;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired private StaffService   staffService;
    @Autowired private UserRepository userRepository;

    // ── Guard — reloads fresh user from DB to avoid lazy proxy issues ─

    private User ownerOrRedirect(HttpSession session, RedirectAttributes ra) {
        User sessionUser = (User) session.getAttribute("loggedInUser");
        if (sessionUser == null || !sessionUser.isOwner()) {
            if (ra != null) ra.addFlashAttribute("error", "Access denied.");
            return null;
        }
        return userRepository.findById(sessionUser.getId()).orElse(null);
    }

    // ── GET /staff ────────────────────────────────────────────────────

    @GetMapping
    public String listStaff(HttpSession session, Model model, RedirectAttributes ra) {
        User owner = ownerOrRedirect(session, ra);
        if (owner == null) return "redirect:/dashboard";

        Long   pharmacyId   = owner.getPharmacy().getId();
        String pharmacyName = owner.getPharmacy().getName();

        List<User> staffList = staffService.getStaffForPharmacy(pharmacyId)
                .stream()
                .filter(u -> u.getPharmacy() != null
                          && u.getPharmacy().getId().equals(pharmacyId))
                .collect(Collectors.toList());

        model.addAttribute("staffList",    staffList);
        model.addAttribute("roles",        new Role[]{Role.PHARMACIST, Role.STAFF});
        model.addAttribute("currentUser",  owner);
        model.addAttribute("pharmacyName", pharmacyName);
        return "pages/staff";
    }

    // ── POST /staff/create — directly create staff account ────────────

    @PostMapping("/create")
    public String createStaff(@RequestParam("username") String username,
                              @RequestParam("email")    String email,
                              @RequestParam("password") String password,
                              @RequestParam("role")     Role   role,
                              HttpSession session,
                              RedirectAttributes ra) {
        User owner = ownerOrRedirect(session, ra);
        if (owner == null) return "redirect:/dashboard";

        if (username == null || username.isBlank()) {
            ra.addFlashAttribute("error", "Full name is required.");
            return "redirect:/staff";
        }
        if (email == null || email.isBlank()) {
            ra.addFlashAttribute("error", "Email is required.");
            return "redirect:/staff";
        }
        if (password == null || password.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/staff";
        }

        try {
            staffService.createStaff(email.trim(), password,
                                     username.trim(), role, owner);
            ra.addFlashAttribute("success",
                    username.trim() + " added successfully as " + role + ".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }

    // ── POST /staff/{id}/role ─────────────────────────────────────────

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam("role") Role role,
                             HttpSession session,
                             RedirectAttributes ra) {
        User owner = ownerOrRedirect(session, ra);
        if (owner == null) return "redirect:/dashboard";

        try {
            staffService.updateRole(id, role, owner);
            ra.addFlashAttribute("success", "Role updated.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }

    // ── POST /staff/{id}/delete ───────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String deleteStaff(@PathVariable Long id,
                              HttpSession session,
                              RedirectAttributes ra) {
        User owner = ownerOrRedirect(session, ra);
        if (owner == null) return "redirect:/dashboard";

        try {
            staffService.deleteStaff(id, owner);
            ra.addFlashAttribute("success", "Staff member removed.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }

    // ── POST /staff/{id}/approve ──────────────────────────────────────

    @PostMapping("/{id}/approve")
    public String approveStaff(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes ra) {
        User owner = ownerOrRedirect(session, ra);
        if (owner == null) return "redirect:/dashboard";

        try {
            staffService.approveStaff(id, owner);
            ra.addFlashAttribute("success", "Staff member approved.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }
}