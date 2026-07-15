package com.myspringboot.SpringBootApp.controller.api;

import com.myspringboot.SpringBootApp.Service.StaffService;
import com.myspringboot.SpringBootApp.model.Role;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;
import com.myspringboot.SpringBootApp.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffRestController {

    private static final int MAX_PHARMACISTS = 1;
    private static final int MAX_STAFF       = 2;

    @Autowired private StaffService staffService;
    @Autowired private UserRepository userRepository;

    private User getAuthenticatedOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            User user = userDetails.getUser();
            if (user.isOwner()) {
                return userRepository.findById(user.getId()).orElse(null);
            }
        }
        return null;
    }

    // ── GET /api/staff ───────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listStaff() {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Only owners can manage staff."));
        }
        Long pharmacyId = owner.getPharmacy().getId();
        List<User> staffList = staffService.getStaffForPharmacy(pharmacyId);
        
        long pharmacistCount = staffList.stream().filter(u -> u.getRole() == Role.PHARMACIST).count();
        long staffCount = staffList.stream().filter(u -> u.getRole() == Role.STAFF).count();

        Map<String, Object> response = new HashMap<>();
        response.put("staffList", staffList);
        response.put("canAddPharmacist", pharmacistCount < MAX_PHARMACISTS);
        response.put("canAddStaff", staffCount < MAX_STAFF);
        
        return ResponseEntity.ok(response);
    }

    // ── GET /api/staff/{id} ──────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getStaffById(@PathVariable Long id) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }
        Long pharmacyId = owner.getPharmacy().getId();
        return userRepository.findById(id)
                .filter(u -> u.getPharmacy() != null && u.getPharmacy().getId().equals(pharmacyId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── POST /api/staff ──────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createStaff(@RequestBody Map<String, String> request) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }

        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        String roleStr = request.get("role");

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Full name is required."));
        }
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 6 characters."));
        }

        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role specified."));
        }

        if (role != Role.PHARMACIST && role != Role.STAFF) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid staff role."));
        }

        Long pharmacyId = owner.getPharmacy().getId();
        List<User> staffList = staffService.getStaffForPharmacy(pharmacyId);

        if (role == Role.PHARMACIST) {
            long count = staffList.stream().filter(u -> u.getRole() == Role.PHARMACIST).count();
            if (count >= MAX_PHARMACISTS) {
                return ResponseEntity.badRequest().body(Map.of("error", "Limit reached: only " + MAX_PHARMACISTS + " pharmacist allowed."));
            }
        } else {
            long count = staffList.stream().filter(u -> u.getRole() == Role.STAFF).count();
            if (count >= MAX_STAFF) {
                return ResponseEntity.badRequest().body(Map.of("error", "Limit reached: only " + MAX_STAFF + " staff members allowed."));
            }
        }

        try {
            staffService.createStaff(email.trim(), password, username.trim(), role, owner);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "message", "Staff created successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT /api/staff/{id} ──────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }
        Long pharmacyId = owner.getPharmacy().getId();
        User staff = userRepository.findById(id)
                .filter(u -> u.getPharmacy() != null && u.getPharmacy().getId().equals(pharmacyId))
                .orElse(null);
        if (staff == null) {
            return ResponseEntity.notFound().build();
        }

        String username = request.get("username");
        String email = request.get("email");
        if (username != null && !username.isBlank()) {
            staff.setUsername(username.trim());
        }
        if (email != null && !email.isBlank()) {
            staff.setEmail(email.trim());
        }

        User saved = userRepository.save(staff);
        return ResponseEntity.ok(saved);
    }

    // ── DELETE /api/staff/{id} ───────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }
        try {
            staffService.deleteStaff(id, owner);
            return ResponseEntity.ok(Map.of("success", true, "message", "Staff member removed."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/staff/{id}/approve ─────────────────────────────────────
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveStaff(@PathVariable Long id) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }
        try {
            staffService.approveStaff(id, owner);
            return ResponseEntity.ok(Map.of("success", true, "message", "Staff member approved."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT /api/staff/{id}/role ─────────────────────────────────────────
    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User owner = getAuthenticatedOwner();
        if (owner == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied."));
        }
        String roleStr = request.get("role");
        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role specified."));
        }

        try {
            staffService.updateRole(id, role, owner);
            return ResponseEntity.ok(Map.of("success", true, "message", "Role updated successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
