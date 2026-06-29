package com.orque.crm.settings.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.settings.dto.UserSettingsDto;
import com.orque.crm.settings.entity.UserSettings;
import com.orque.crm.settings.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository repository;

    public UserSettingsDto getForCurrentUser() {
        String username = UserContextHelper.currentUsername();
        UserSettings settings = repository.findByUsername(username)
                .orElseGet(() -> defaultSettings(username));
        return toDto(settings);
    }

    public UserSettingsDto save(UserSettingsDto dto) {
        String username = UserContextHelper.currentUsername();
        UserSettings settings = repository.findByUsername(username)
                .orElseGet(() -> UserSettings.builder().username(username).build());

        applyDto(settings, dto);
        return toDto(repository.save(settings));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UserSettings defaultSettings(String username) {
        return UserSettings.builder()
                .username(username)
                .mailSslEnabled(true)
                .notifyTaskDue(true)
                .notifyDealStageChange(true)
                .notifyLeadAssigned(true)
                .notifyQuoteApproved(true)
                .notifyInvoicePaid(true)
                .notifyFollowupReminder(true)
                .calendarSyncEnabled(false)
                .calendarProvider("NONE")
                .followupReminderDays(1)
                .campaignUpdatesEnabled(true)
                .dailyDigestEnabled(false)
                .digestTime("08:00")
                .quoteSeriesPrefix("Q-")
                .quoteNextNumber(1001)
                .invoiceSeriesPrefix("INV-")
                .invoiceNextNumber(1001)
                .defaultPrinter("")
                .build();
    }

    private void applyDto(UserSettings s, UserSettingsDto d) {
        if (d.getMailHost()          != null) s.setMailHost(d.getMailHost());
        if (d.getMailPort()          != null) s.setMailPort(d.getMailPort());
        if (d.getMailUsername()      != null) s.setMailUsername(d.getMailUsername());
        if (d.getMailFromName()      != null) s.setMailFromName(d.getMailFromName());
        if (d.getMailFromAddress()   != null) s.setMailFromAddress(d.getMailFromAddress());
        if (d.getMailSslEnabled()    != null) s.setMailSslEnabled(d.getMailSslEnabled());
        if (d.getMailSignature()     != null) s.setMailSignature(d.getMailSignature());

        if (d.getNotifyTaskDue()          != null) s.setNotifyTaskDue(d.getNotifyTaskDue());
        if (d.getNotifyDealStageChange()  != null) s.setNotifyDealStageChange(d.getNotifyDealStageChange());
        if (d.getNotifyLeadAssigned()     != null) s.setNotifyLeadAssigned(d.getNotifyLeadAssigned());
        if (d.getNotifyQuoteApproved()    != null) s.setNotifyQuoteApproved(d.getNotifyQuoteApproved());
        if (d.getNotifyInvoicePaid()      != null) s.setNotifyInvoicePaid(d.getNotifyInvoicePaid());
        if (d.getNotifyFollowupReminder() != null) s.setNotifyFollowupReminder(d.getNotifyFollowupReminder());

        if (d.getCalendarSyncEnabled()   != null) s.setCalendarSyncEnabled(d.getCalendarSyncEnabled());
        if (d.getCalendarProvider()      != null) s.setCalendarProvider(d.getCalendarProvider());
        if (d.getFollowupReminderDays()  != null) s.setFollowupReminderDays(d.getFollowupReminderDays());

        if (d.getCampaignUpdatesEnabled() != null) s.setCampaignUpdatesEnabled(d.getCampaignUpdatesEnabled());
        if (d.getDailyDigestEnabled()     != null) s.setDailyDigestEnabled(d.getDailyDigestEnabled());
        if (d.getDigestTime()             != null) s.setDigestTime(d.getDigestTime());

        if (d.getQuoteSeriesPrefix()      != null) s.setQuoteSeriesPrefix(d.getQuoteSeriesPrefix());
        if (d.getQuoteNextNumber()        != null) s.setQuoteNextNumber(d.getQuoteNextNumber());
        if (d.getInvoiceSeriesPrefix()    != null) s.setInvoiceSeriesPrefix(d.getInvoiceSeriesPrefix());
        if (d.getInvoiceNextNumber()      != null) s.setInvoiceNextNumber(d.getInvoiceNextNumber());

        if (d.getDefaultPrinter()         != null) s.setDefaultPrinter(d.getDefaultPrinter());
    }

    private UserSettingsDto toDto(UserSettings s) {
        return UserSettingsDto.builder()
                .id(s.getId())
                .username(s.getUsername())
                .mailHost(s.getMailHost())
                .mailPort(s.getMailPort())
                .mailUsername(s.getMailUsername())
                .mailFromName(s.getMailFromName())
                .mailFromAddress(s.getMailFromAddress())
                .mailSslEnabled(s.getMailSslEnabled())
                .mailSignature(s.getMailSignature())
                .notifyTaskDue(s.getNotifyTaskDue())
                .notifyDealStageChange(s.getNotifyDealStageChange())
                .notifyLeadAssigned(s.getNotifyLeadAssigned())
                .notifyQuoteApproved(s.getNotifyQuoteApproved())
                .notifyInvoicePaid(s.getNotifyInvoicePaid())
                .notifyFollowupReminder(s.getNotifyFollowupReminder())
                .calendarSyncEnabled(s.getCalendarSyncEnabled())
                .calendarProvider(s.getCalendarProvider())
                .followupReminderDays(s.getFollowupReminderDays())
                .campaignUpdatesEnabled(s.getCampaignUpdatesEnabled())
                .dailyDigestEnabled(s.getDailyDigestEnabled())
                .digestTime(s.getDigestTime())
                .quoteSeriesPrefix(s.getQuoteSeriesPrefix())
                .quoteNextNumber(s.getQuoteNextNumber())
                .invoiceSeriesPrefix(s.getInvoiceSeriesPrefix())
                .invoiceNextNumber(s.getInvoiceNextNumber())
                .defaultPrinter(s.getDefaultPrinter())
                .build();
    }

    public synchronized String getAndIncrementQuoteNumber() {
        String username = UserContextHelper.currentUsername();
        UserSettings s = repository.findByUsername(username)
                .orElseGet(() -> defaultSettings(username));
        
        String prefix = s.getQuoteSeriesPrefix() != null ? s.getQuoteSeriesPrefix() : "Q-";
        int next = s.getQuoteNextNumber() != null ? s.getQuoteNextNumber() : 1001;
        
        s.setQuoteSeriesPrefix(prefix);
        s.setQuoteNextNumber(next + 1);
        repository.save(s);
        
        return prefix + next;
    }

    public synchronized String getAndIncrementInvoiceNumber() {
        String username = UserContextHelper.currentUsername();
        UserSettings s = repository.findByUsername(username)
                .orElseGet(() -> defaultSettings(username));
        
        String prefix = s.getInvoiceSeriesPrefix() != null ? s.getInvoiceSeriesPrefix() : "INV-";
        int next = s.getInvoiceNextNumber() != null ? s.getInvoiceNextNumber() : 1001;
        
        s.setInvoiceSeriesPrefix(prefix);
        s.setInvoiceNextNumber(next + 1);
        repository.save(s);
        
        return prefix + next;
    }
}
