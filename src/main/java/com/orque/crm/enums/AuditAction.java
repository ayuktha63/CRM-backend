package com.orque.crm.enums;

public enum AuditAction {

    // Authentication
    USER_LOGIN,
    USER_LOGOUT,

    // Contacts
    CONTACT_IMPORTED,

    // Leads
    LEAD_CREATED,
    LEAD_CONVERTED,
    CONTACT_CONVERTED_TO_LEAD,
    BULK_CONTACT_CONVERTED_TO_LEAD,
    LEAD_STATUS_CHANGED,

    // Tasks
    TASK_CREATED,
    TASK_COMPLETED,

    // Email
    EMAIL_SENT,
    EMAIL_RECEIVED,
    EMAIL_ACTIVITY,

    // Campaigns
    CAMPAIGN_CREATED,
    CAMPAIGN_LAUNCHED,

    // CSV
    CSV_IMPORTED,

    // Reports
    REPORT_EXPORTED,

    // System
    SYSTEM_ERROR
}