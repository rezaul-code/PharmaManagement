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
 * IMPORTANT: Defaults to null. If a service reads null, it means the
 * tenant context was never initialised — callers must throw rather
 * than silently operating on another pharmacy's data.
 */
public class TenantContext {

    /** Legacy constant — only used during signup to seed the first pharmacy row. */
    public static final Long DEFAULT_PHARMACY_ID = 1L;

    private static final ThreadLocal<Long> currentPharmacyId =
            ThreadLocal.withInitial(() -> null);

    private TenantContext() { /* utility class */ }

    public static Long getCurrentPharmacyId() {
        return currentPharmacyId.get();
    }

    public static void setCurrentPharmacyId(Long pharmacyId) {
        currentPharmacyId.set(pharmacyId);
    }

    /** Must be called at the end of every request to prevent thread-pool leaks. */
    public static void clear() {
        currentPharmacyId.remove();
    }
}