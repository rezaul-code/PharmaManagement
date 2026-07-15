package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.AuthService;
import org.springframework.transaction.annotation.Transactional;
import com.myspringboot.SpringBootApp.Service.TenantRegistrationService;
import com.myspringboot.SpringBootApp.Service.TenantContext;
import com.myspringboot.SpringBootApp.dto.AuthResponse;
import com.myspringboot.SpringBootApp.dto.LoginDto;
import com.myspringboot.SpringBootApp.dto.RegistrationDto;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private TenantRegistrationService tenantRegistrationService;

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginDto loginDto,
            jakarta.servlet.http.HttpServletResponse response) {
        
        AuthResponse authRes = authService.authenticate(loginDto);
        
        // Set JWT token in an HttpOnly cookie for hybrid/backward-compatibility
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", authRes.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 1 day
        response.addCookie(cookie);

        return ResponseEntity.ok(authRes);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        
        // Clear JWT cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", "Logged out successfully");
        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/auth/me")
    @Transactional(readOnly = true)
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", "Not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
        }
        
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            User user = userDetails.getUser();
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("phone", user.getPhone());
            userData.put("role", user.getRole().name());
            userData.put("status", user.getStatus());
            userData.put("pharmacyId", user.getPharmacy() != null ? user.getPharmacy().getId() : null);
            userData.put("pharmacyName", user.getPharmacy() != null ? user.getPharmacy().getName() : "Super Admin");
            return ResponseEntity.ok(userData);
        }
        
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("success", false);
        errorBody.put("message", "Invalid principal type");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody);
    }

    @PostMapping({"/auth/register", "/auth/register-pharmacy"})
    public ResponseEntity<?> registerTenant(@Valid @RequestBody RegistrationDto req) {
        try {
            Pharmacy p = tenantRegistrationService.registerPharmacy(req);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Successfully registered pharmacy " + p.getName());
            responseBody.put("pharmacyName", p.getName());
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("success", false);
            errorBody.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }
}
