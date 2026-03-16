package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.dto.AuthResponse;
import com.myspringboot.SpringBootApp.dto.LoginDto;
import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.security.CustomUserDetails;
import com.myspringboot.SpringBootApp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;

    public AuthResponse authenticate(LoginDto loginDto) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getIdentifier(), loginDto.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();
        Pharmacy pharmacy = user.getPharmacy();

        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getRole().name(),
                pharmacy != null ? pharmacy.getTenantId() : null,
                pharmacy != null ? pharmacy.getId() : null,
                pharmacy != null ? pharmacy.getName() : "Super Admin",
                user.getUsername()
        );
    }
}
