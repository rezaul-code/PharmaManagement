package com.myspringboot.SpringBootApp.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

import com.myspringboot.SpringBootApp.model.User;
import com.myspringboot.SpringBootApp.repo.UserRepository;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepository;

    // Show login form
    @GetMapping("/login")
    public String loginForm() {
        return "user_auth/user_login"; 
    }
    
    @GetMapping("/")
    public String root() {
        return "user_auth/index";
    }

    // Handle login
    @PostMapping("/login")
    public String loginUser(@RequestParam String email,
                            @RequestParam String password,
                            HttpServletRequest request,
                            Model model) {

        User user = userRepository.findByEmail(email);

        if (user != null && user.getPassword().equals(password)) {
            // ✅ Create session
            HttpSession session = request.getSession();
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());

            return "redirect:/dashboard"; 
        }

        model.addAttribute("error", "Invalid email or password");
        return "user_auth/user_login";
    }

    // Dashboard (protected page)
    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request,
    		HttpServletResponse response ) {
    	
    	 // Prevent browser from caching secured pages
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); 
        response.setHeader("Pragma", "no-cache"); 
        response.setDateHeader("Expires", 0);
    	
    	
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            return "redirect:/login"; // Not logged in → back to login
        }

        return "pages/dashboard"; 
    }
    
    @GetMapping("/index")
    public String index(HttpServletRequest request) {
    	
    	return "user_auth/index";
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate(); // Destroy session
        }

        return "redirect:/index";
    }
}
