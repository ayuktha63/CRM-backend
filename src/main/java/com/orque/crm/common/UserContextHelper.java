package com.orque.crm.common;

import com.orque.crm.auth.entity.User;
import com.orque.crm.enums.RoleType;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Central access-control helper.
 *
 * Access levels (highest → lowest):
 *   SYSTEM_ADMIN  – all orgs, all data, bypass every filter
 *   ADMIN         – their org only, all users in org
 *   SALES_ADMIN   – their org only, all users in org
 *   SALES         – their org only, own records only
 *   SALES_USER    – their org only, own records only
 */
public final class UserContextHelper {

    private UserContextHelper() {}

    // ──────────────────────────────────────────────────────────────────────────
    // Principal helpers
    // ──────────────────────────────────────────────────────────────────────────

    public static User currentUser() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (p instanceof User u) ? u : null;
    }

    public static String currentUsername() {
        User u = currentUser();
        return u != null ? u.getUsername() : "system";
    }

    public static String currentOrganizationId() {
        User u = currentUser();
        return u != null ? u.getOrganizationId() : null;
    }

    public static RoleType currentRole() {
        User u = currentUser();
        if (u == null || u.getRole() == null) return RoleType.SALES_USER;
        try {
            return RoleType.valueOf(u.getRole().getName().name());
        } catch (Exception e) {
            return RoleType.SALES_USER;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Role checks
    // ──────────────────────────────────────────────────────────────────────────

    /** True for SYSTEM_ADMIN — bypasses all org/owner filters. */
    public static boolean isSystemAdmin() {
        return currentRole() == RoleType.SYSTEM_ADMIN;
    }

    /** True for ADMIN or SALES_ADMIN — sees all records in their org. */
    public static boolean isOrganizationAdmin() {
        RoleType r = currentRole();
        return r == RoleType.ADMIN || r == RoleType.SALES_ADMIN;
    }

    /** True if the current user has any admin capability (org OR system). */
    public static boolean isAdmin() {
        return isSystemAdmin() || isOrganizationAdmin();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Access checks
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Can the current user read a record owned by {@code recordOwner} inside
     * {@code recordOrgId}?
     *
     * <pre>
     * SYSTEM_ADMIN  → always true
     * ORG ADMIN     → true if recordOrgId == currentOrgId
     * SALES user    → true if recordOrgId == currentOrgId AND owner == currentUser
     * </pre>
     */
    public static boolean canAccess(String recordOrgId, String recordOwner) {
        if (isSystemAdmin()) return true;

        // Must be same org
        String myOrg = currentOrganizationId();
        if (myOrg == null || !myOrg.equals(recordOrgId)) {
            // Allow null org records during migration (legacy data)
            if (recordOrgId != null) return false;
        }

        if (isOrganizationAdmin()) return true;

        // Sales user: also check owner
        if (recordOwner == null || recordOwner.isBlank()) return false;
        return recordOwner.equalsIgnoreCase(currentUsername());
    }

    /**
     * Backward-compatible single-arg version (org isolation already applied by
     * repository; only owner check needed here).
     */
    public static boolean canAccess(String recordOwner) {
        if (isSystemAdmin()) return true;
        if (isOrganizationAdmin()) return true;
        if (recordOwner == null || recordOwner.isBlank()) return false;
        return recordOwner.equalsIgnoreCase(currentUsername());
    }

    public static void assertAccess(String recordOrgId, String recordOwner) {
        if (!canAccess(recordOrgId, recordOwner)) {
            throw new RuntimeException("Access denied.");
        }
    }

    public static void assertAccess(String recordOwner) {
        if (!canAccess(recordOwner)) {
            throw new RuntimeException("Access denied.");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scope helpers for repositories
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the org-id to scope queries to.
     * SYSTEM_ADMIN in CRM is the tenant's own admin — still org-scoped.
     * There is no cross-tenant actor in CRM; Orque admin never logs in here.
     */
    public static String scopedOrgId() {
        return currentOrganizationId();
    }

    /**
     * Returns the owner username to further scope queries to, or null if the
     * current user can see all owners within their org (org admins).
     */
    public static String scopedOwner() {
        if (isSystemAdmin() || isOrganizationAdmin()) return null;
        return currentUsername();
    }
}
