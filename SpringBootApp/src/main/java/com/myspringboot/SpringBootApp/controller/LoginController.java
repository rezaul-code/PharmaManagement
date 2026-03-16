package com.myspringboot.SpringBootApp.controller;

import com.myspringboot.SpringBootApp.Service.AuthService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;

@Controller
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired private UserService userService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @Autowired private AuthService authService;

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
            jakarta.servlet.http.HttpServletResponse response,
            RedirectAttributes ra) {

        try {
            com.myspringboot.SpringBootApp.dto.LoginDto loginDto = new com.myspringboot.SpringBootApp.dto.LoginDto();
            loginDto.setIdentifier(identifier);
            loginDto.setPassword(password);
            
            com.myspringboot.SpringBootApp.dto.AuthResponse authRes = authService.authenticate(loginDto);
            
            // Set JWT token in an HttpOnly cookie
            jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", authRes.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 1 day
            response.addCookie(cookie);

            // Fetch user for session backward-compatibility
            User user = userService.findByIdentifierGlobal(identifier);
            
            // Account status check
            String status = user.getStatus();
            if ("PENDING".equalsIgnoreCase(status)) {
                log.info("LOGIN_FAIL | userId={} | reason=PENDING_ACCOUNT", user.getId());
                ra.addFlashAttribute("error", "Your account is pending approval. Please contact your pharmacy owner.");
                return "redirect:/login";
            }
            if (!"ACTIVE".equalsIgnoreCase(status)) {
                log.warn("LOGIN_FAIL | userId={} | reason=INACTIVE_STATUS status='{}'", user.getId(), status);
                ra.addFlashAttribute("error", "Your account is not active. Please contact your pharmacy owner.");
                return "redirect:/login";
            }

            session.setAttribute("loggedInUser", user);
            session.setAttribute("pharmacyId", user.getPharmacy() != null ? user.getPharmacy().getId() : null);

            // Set Spring Security Authentication Context
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("LOGIN_OK | userId={} pharmacyId={}", user.getId(), user.getPharmacy() != null ? user.getPharmacy().getId() : null);
            return "redirect:/dashboard";

        } catch (Exception e) {
            log.warn("LOGIN_FAIL | identifier='{}' | reason={}", identifier, e.getMessage());
            ra.addFlashAttribute("error", "Invalid email/phone or password.");
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, jakarta.servlet.http.HttpServletResponse response) {
        session.invalidate();
        TenantContext.clear();
        
        // Clear JWT cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("jwt", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
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