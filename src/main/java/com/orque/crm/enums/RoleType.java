package com.orque.crm.enums;

public enum RoleType {
    SYSTEM_ADMIN,   // platform-level, bypasses all org/license filters
    ADMIN,          // organization admin — all data within their org
    SALES_ADMIN,    // org sales admin
    SALES,
    SALES_USER
}