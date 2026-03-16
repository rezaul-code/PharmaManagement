package com.myspringboot.SpringBootApp.security;

import com.myspringboot.SpringBootApp.Service.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null) {
                try {
                    String username = jwtUtil.extractUsername(jwt);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        if (jwtUtil.validateToken(jwt, (CustomUserDetails) userDetails)) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            // Set TenantContext from JWT
                            Long pharmacyId = jwtUtil.extractClaim(jwt, claims -> claims.get("pharmacyId", Long.class));
                            if (pharmacyId != null) {
                                TenantContext.setCurrentPharmacyId(pharmacyId);
                            } else {
                                // Default for superadmin or before pharmacy assignment
                                TenantContext.setCurrentPharmacyId(TenantContext.DEFAULT_PHARMACY_ID);
                            }
                        }
                    }
                } catch (io.jsonwebtoken.JwtException | IllegalArgumentException | org.springframework.security.core.userdetails.UsernameNotFoundException e) {
                    // Log the exception but do not break the filter chain. Request simply goes unauthenticated.
                    logger.warn("JWT Verification or User Lookup failed: " + e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            } else {
                 // Not authenticated but allowed? Still define tenant context default so services don't crash
                 TenantContext.setCurrentPharmacyId(TenantContext.DEFAULT_PHARMACY_ID);
            }

            chain.doFilter(request, response);
        } finally {
            // Clean up tenant context after request
            TenantContext.clear();
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
