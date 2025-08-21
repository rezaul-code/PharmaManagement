package com.myspringboot.SpringBootApp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        // Allowed without login
        String uri = request.getRequestURI();
        if (uri.equals("/") || uri.startsWith("/index") ||uri.startsWith("/login") || uri.startsWith("/signup") || uri.startsWith("/css") || uri.startsWith("/js")) {
            return true; // skip session check
        }

        // Check if user is logged in
        if (session != null && session.getAttribute("userId") != null) {
            return true; // user is logged in
        }

        // Redirect if not logged in
        response.sendRedirect("/login");
        return false;
    }
}
