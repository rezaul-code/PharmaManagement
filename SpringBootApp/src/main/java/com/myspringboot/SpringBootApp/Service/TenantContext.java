package com.myspringboot.SpringBootApp.Service;

/**
 * Holds the current pharmacy ID in a thread-local variable so any
 * service can call TenantContext.getCurrentPharmacyId() without
 * needing an HttpSession reference.
 *
 * Lifecycle:
 *  - Set by TenantInterceptor.preHandle()  (after auth passes)
 *  - Cleared by TenantInterceptor.afterCompletion()  (always)
 *
 * DEFAULT_PHARMACY_ID (1L) is used as a safe fallback so that
 * data created before multi-tenancy was added still resolves
 * to a real pharmacy row.
 */
public class TenantContext {

    /** Fallback pharmacy ID — matches the first row inserted in the pharmacies table. */
    public static final Long DEFAULT_PHARMACY_ID = 1L;

    private static final ThreadLocal<Long> currentPharmacyId =
            ThreadLocal.withInitial(() -> DEFAULT_PHARMACY_ID);

    private TenantContext() { /* utility class */ }

    public static Long getCurrentPharmacyId() {
        return currentPharmacyId.get();
    }

    public static void setCurrentPharmacyId(Long pharmacyId) {
        currentPharmacyId.set(
                pharmacyId != null ? pharmacyId : DEFAULT_PHARMACY_ID
        );
    }

    /** Must be called at the end of every request to prevent thread-pool leaks. */
    public static void clear() {
        currentPharmacyId.remove();
    }
}