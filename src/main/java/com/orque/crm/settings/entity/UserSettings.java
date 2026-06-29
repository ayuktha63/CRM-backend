package com.orque.crm.settings.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // ── Mail / SMTP ──────────────────────────────────────────────────────────
    private String mailHost;
    private Integer mailPort;
    private String mailUsername;
    private String mailPassword;       // stored encrypted in production
    private String mailFromName;
    private String mailFromAddress;
    private Boolean mailSslEnabled;
    @Column(columnDefinition = "TEXT")
    private String mailSignature;

    // ── Notification Preferences ─────────────────────────────────────────────
    private Boolean notifyTaskDue;
    private Boolean notifyDealStageChange;
    private Boolean notifyLeadAssigned;
    private Boolean notifyQuoteApproved;
    private Boolean notifyInvoicePaid;
    private Boolean notifyFollowupReminder;

    // ── Calendar ─────────────────────────────────────────────────────────────
    private Boolean calendarSyncEnabled;
    private String  calendarProvider;  // GOOGLE, OUTLOOK, NONE
    private Integer followupReminderDays;

    // ── Campaign ─────────────────────────────────────────────────────────────
    private Boolean campaignUpdatesEnabled;
    private Boolean dailyDigestEnabled;
    private String  digestTime;        // e.g. "08:00"

    // ── Document Numbering Series ────────────────────────────────────────────
    private String  quoteSeriesPrefix;
    private Integer quoteNextNumber;
    private String  invoiceSeriesPrefix;
    private Integer invoiceNextNumber;

    private LocalDateTime updatedAt;

    @PrePersist @PreUpdate
    protected void touch() { updatedAt = LocalDateTime.now(); }
}
