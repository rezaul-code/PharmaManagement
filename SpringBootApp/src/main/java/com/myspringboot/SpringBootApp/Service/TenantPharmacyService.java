package com.myspringboot.SpringBootApp.Service;

import com.myspringboot.SpringBootApp.model.Pharmacy;
import com.myspringboot.SpringBootApp.repo.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Central service used by MedicineService, BillingService, etc.
 * to resolve the current tenant's pharmacy without touching HttpSession.
 *
 * It reads the pharmacy ID from TenantContext (which TenantInterceptor
 * populates from the session on every request).
 */
@Service
public class TenantPharmacyService {

    @Autowired
    private PharmacyRepository pharmacyRepository;

    /**
     * Returns the pharmacy ID for the current request's tenant.
     * Throws if tenant context was never initialized — prevents silent
     * cross-tenant data leaks.
     */
    public Long getCurrentPharmacyId() {
        Long id = TenantContext.getCurrentPharmacyId();
        if (id == null) {
            throw new IllegalStateException(
                    "Tenant context not initialized. "
                  + "Ensure the user is logged in and TenantInterceptor has run.");
        }
        return id;
    }

    /**
     * Returns the full Pharmacy entity for the current tenant.
     * Throws if the pharmacy row does not exist — which should never
     * happen if data is seeded correctly on first run.
     */
    public Pharmacy getCurrentPharmacy() {
        Long pharmacyId = getCurrentPharmacyId();
        return pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new IllegalStateException(
                        "Pharmacy not found for id: " + pharmacyId +
                        ". Please ensure the pharmacies table is seeded correctly."));
    }

    /**
     * Returns the default pharmacy (id = 1).
     * Used during signup and other flows where no session exists yet.
     */
    public Pharmacy getDefaultPharmacy() {
        return getPharmacyById(TenantContext.DEFAULT_PHARMACY_ID);
    }

    /**
     * Convenience: look up any pharmacy by ID (e.g. admin use).
     */
    public Pharmacy getPharmacyById(Long id) {
        return pharmacyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pharmacy not found with id: " + id));
    }
}