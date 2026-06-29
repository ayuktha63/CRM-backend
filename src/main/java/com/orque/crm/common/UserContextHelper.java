package com.orque.crm.common;

import com.orque.crm.auth.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

public final class UserContextHelper {

    private UserContextHelper() {}

    public static String currentUsername() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (p instanceof User u) ? u.getUsername() : "admin";
    }

    public static boolean isAdmin() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof User u) {
            String r = u.getRole() != null ? u.getRole().getName().name() : "";
            return "ADMIN".equals(r) || "SALES_ADMIN".equals(r);
        }
        return false;
    }

    /** True if the record is owned by the current user. Null-owner records are admin-only. */
    public static boolean canAccess(String owner) {
        if (isAdmin()) return true;
        if (owner == null || owner.isBlank()) return false;
        return owner.equalsIgnoreCase(currentUsername());
    }

    public static void assertAccess(String owner) {
        if (!canAccess(owner)) throw new RuntimeException("Access denied.");
    }
}
