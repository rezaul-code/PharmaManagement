package com.myspringboot.SpringBootApp.config;

import com.myspringboot.SpringBootApp.Service.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs AFTER AuthInterceptor confirms the user is logged in.
 * Reads pharmacyId from the session and loads it into TenantContext
 * so services can call TenantContext.getCurrentPharmacyId() freely.
 *
 * afterCompletion() always clears the thread-local — critical for
 * thread-pool reuse safety.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object pharmacyIdAttr = session.getAttribute("pharmacyId");
            if (pharmacyIdAttr instanceof Long pharmacyId) {
                TenantContext.setCurrentPharmacyId(pharmacyId);
            } else {
                // Session exists but pharmacyId not set yet (e.g. login/signup page)
                TenantContext.setCurrentPharmacyId(TenantContext.DEFAULT_PHARMACY_ID);
            }
        } else {
            TenantContext.setCurrentPharmacyId(TenantContext.DEFAULT_PHARMACY_ID);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // Always clear to prevent leaking pharmacyId across requests
        // on the same thread (important with thread pools).
        TenantContext.clear();
    }
}