package com.orque.crm.enums;

public enum LicenseStatus {
    ACTIVE,
    GRACE,      // past end date but within grace period — warn user, allow access
    EXPIRED,    // past grace period — block access
    SUSPENDED
}
