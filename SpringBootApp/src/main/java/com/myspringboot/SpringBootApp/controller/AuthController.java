package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.AuthService;
import com.myspringboot.SpringBootApp.Service.TenantRegistrationService;
import com.myspringboot.SpringBootApp.dto.AuthResponse;
import com.myspringboot.SpringBootApp.dto.LoginDto;
import com.myspringboot.SpringBootApp.dto.RegistrationDto;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private TenantRegistrationService tenantRegistrationService;

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(authService.authenticate(loginDto));
    }

    @PostMapping("/auth/register-pharmacy")
    public ResponseEntity<?> registerTenant(@Valid @RequestBody RegistrationDto req) {
        try {
            Pharmacy p = tenantRegistrationService.registerPharmacy(req);
            return ResponseEntity.ok("Successfully registered pharmacy " + p.getName());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
