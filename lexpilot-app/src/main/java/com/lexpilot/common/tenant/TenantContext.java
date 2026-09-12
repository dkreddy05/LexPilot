package com.lexpilot.common.tenant;

import java.util.UUID;

/**
 * Thread-local holder for the current request's tenant ID.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    private TenantContext() {}

    /**
     * Set the current tenant ID.
     */
    public static void setTenantId(UUID tenantId) {
        currentTenant.set(tenantId);
    }

    /**
     * Get the current tenant ID.
     */
    public static UUID getTenantId() {
        return currentTenant.get();
    }

    /**
     * Clear the current tenant context. Essential for preventing leakages in pooled threads.
     */
    public static void clear() {
        currentTenant.remove();
    }
}
