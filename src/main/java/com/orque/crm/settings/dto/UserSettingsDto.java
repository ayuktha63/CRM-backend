package com.orque.crm.settings.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSettingsDto {

    private Long    id;
    private String  username;

    // Mail
    private String  mailHost;
    private Integer mailPort;
    private String  mailUsername;
    private String  mailPassword;
    private String  mailFromName;
    private String  mailFromAddress;
    private Boolean mailSslEnabled;
    private String  mailSignature;

    // Notifications
    private Boolean notifyTaskDue;
    private Boolean notifyDealStageChange;
    private Boolean notifyLeadAssigned;
    private Boolean notifyQuoteApproved;
    private Boolean notifyInvoicePaid;
    private Boolean notifyFollowupReminder;

    // Calendar
    private Boolean calendarSyncEnabled;
    private String  calendarProvider;
    private Integer followupReminderDays;

    // Campaign
    private Boolean campaignUpdatesEnabled;
    private Boolean dailyDigestEnabled;
    private String  digestTime;

    // Series
    private String  quoteSeriesPrefix;
    private Integer quoteNextNumber;
    private String  invoiceSeriesPrefix;
    private Integer invoiceNextNumber;

    // Printer
    private String  defaultPrinter;
}
