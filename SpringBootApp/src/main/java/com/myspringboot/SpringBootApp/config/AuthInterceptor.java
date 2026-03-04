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

        String uri = request.getRequestURI();

        // ── Always allow public routes ────────────────────────────────
        if (isPublicUri(uri)) {
            return true;
        }

        // ── Check session for logged-in user ─────────────────────────
        HttpSession session = request.getSession(false);

        if (session != null
                // "loggedInUser" → set by upgraded LoginController (full User object)
                && (session.getAttribute("loggedInUser") != null
                // "userId"       → set by original LoginController (Long id)
                //  Kept for backward-compatibility during migration.
                ||  session.getAttribute("userId") != null)) {
            return true;   // ✅ authenticated
        }

        // ── Not logged in → redirect ──────────────────────────────────
        response.sendRedirect("/login");
        return false;
    }

    /**
     * Returns true for any URI that must be accessible without a session.
     * Centralised here so WebConfig.excludePathPatterns() and this check
     * stay in sync automatically.
     */
    private boolean isPublicUri(String uri) {
        return uri.equals("/")
            || uri.startsWith("/index")
            || uri.startsWith("/login")
            || uri.startsWith("/signup")
            || uri.startsWith("/css/")
            || uri.startsWith("/js/")
            || uri.startsWith("/images/")
            || uri.startsWith("/favicon")
            || uri.startsWith("/error");   // Spring's default error page
    }
}