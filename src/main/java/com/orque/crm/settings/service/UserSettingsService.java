package com.orque.crm.settings.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Invoice;
import com.orque.crm.feature.entity.Quote;
import com.orque.crm.feature.repository.InvoiceRepository;
import com.orque.crm.feature.repository.QuoteRepository;
import com.orque.crm.organization.entity.Organization;
import com.orque.crm.organization.repository.OrganizationRepository;
import com.orque.crm.settings.dto.UserSettingsDto;
import com.orque.crm.settings.entity.UserSettings;
import com.orque.crm.settings.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingsService {

    private final UserSettingsRepository repository;
    private final OrganizationRepository organizationRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;

    public UserSettingsDto getForCurrentUser() {
        String username = UserContextHelper.currentUsername();
        UserSettings settings = repository.findByUsername(username)
                .orElseGet(() -> defaultSettings(username));
        UserSettingsDto dto = toDto(settings);
        // Document numbering is a tenant-wide policy, not a personal preference — every
        // user in the org must see (and share) the same series/counter, sourced from
        // Organization rather than this per-user row.
        Organization org = currentOrganization();
        if (org != null) {
            dto.setQuoteSeriesPrefix(org.getQuoteSeriesPrefix() != null ? org.getQuoteSeriesPrefix() : "Q-");
            dto.setQuoteNextNumber(org.getQuoteNextNumber() != null ? org.getQuoteNextNumber() : 1001);
            dto.setInvoiceSeriesPrefix(org.getInvoiceSeriesPrefix() != null ? org.getInvoiceSeriesPrefix() : "INV-");
            dto.setInvoiceNextNumber(org.getInvoiceNextNumber() != null ? org.getInvoiceNextNumber() : 1001);
        }
        return dto;
    }

    public UserSettingsDto save(UserSettingsDto dto) {
        String username = UserContextHelper.currentUsername();
        UserSettings settings = repository.findByUsername(username)
                .orElseGet(() -> UserSettings.builder().username(username).build());

        applyDto(settings, dto);
        UserSettingsDto saved = toDto(repository.save(settings));

        // Numbering fields are org-scoped and SYSTEM_ADMIN-only — silently ignored (not
        // an error) for any other role, so a business user's unrelated settings (mail,
        // notifications) still save fine even if their payload echoes back these fields.
        Organization org = currentOrganization();
        if (org != null && UserContextHelper.isSystemAdmin()) {
            boolean changed = false;
            if (dto.getQuoteSeriesPrefix() != null) { org.setQuoteSeriesPrefix(dto.getQuoteSeriesPrefix()); changed = true; }
            if (dto.getQuoteNextNumber() != null) { org.setQuoteNextNumber(dto.getQuoteNextNumber()); changed = true; }
            if (dto.getInvoiceSeriesPrefix() != null) { org.setInvoiceSeriesPrefix(dto.getInvoiceSeriesPrefix()); changed = true; }
            if (dto.getInvoiceNextNumber() != null) { org.setInvoiceNextNumber(dto.getInvoiceNextNumber()); changed = true; }
            if (changed) organizationRepository.save(org);
        }
        if (org != null) {
            saved.setQuoteSeriesPrefix(org.getQuoteSeriesPrefix());
            saved.setQuoteNextNumber(org.getQuoteNextNumber());
            saved.setInvoiceSeriesPrefix(org.getInvoiceSeriesPrefix());
            saved.setInvoiceNextNumber(org.getInvoiceNextNumber());
        }
        return saved;
    }

    private Organization currentOrganization() {
        String orgId = UserContextHelper.currentOrganizationId();
        return orgId != null ? organizationRepository.findById(orgId).orElse(null) : null;
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
        if (d.getMailPassword()      != null) s.setMailPassword(d.getMailPassword());
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
                .mailPassword(s.getMailPassword())
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

    /**
     * Org-scoped, not per-user: quote/invoice numbering is one continuous series per
     * tenant. Keying this by username (the old behavior) meant two sales reps in the
     * same org each got their own counter — both would produce "Q-1001" as their first
     * quote, a real numbering collision within a single tenant.
     */
    public synchronized String getAndIncrementQuoteNumber() {
        Organization org = requireCurrentOrganization();

        String prefix = org.getQuoteSeriesPrefix() != null ? org.getQuoteSeriesPrefix() : "Q-";
        int next = org.getQuoteNextNumber() != null ? org.getQuoteNextNumber() : firstQuoteNumberFor(org, prefix);

        org.setQuoteSeriesPrefix(prefix);
        org.setQuoteNextNumber(next + 1);
        organizationRepository.save(org);

        return prefix + next;
    }

    public synchronized String getAndIncrementInvoiceNumber() {
        Organization org = requireCurrentOrganization();

        String prefix = org.getInvoiceSeriesPrefix() != null ? org.getInvoiceSeriesPrefix() : "INV-";
        int next = org.getInvoiceNextNumber() != null ? org.getInvoiceNextNumber() : firstInvoiceNumberFor(org, prefix);

        org.setInvoiceSeriesPrefix(prefix);
        org.setInvoiceNextNumber(next + 1);
        organizationRepository.save(org);

        return prefix + next;
    }

    /**
     * The counter column is new — every tenant that already has quotes starts with it
     * NULL. Defaulting straight to 1001 in that case would reissue numbers that already
     * exist (e.g. a tenant already at Q-1050 would suddenly get a NEW Q-1001, colliding
     * with their own real Q-1001). Scans existing quotes for this org once, and seeds the
     * counter one past the highest number actually in use.
     */
    private int firstQuoteNumberFor(Organization org, String prefix) {
        int max = 1000;
        for (Quote q : quoteRepository.findByOrganizationId(org.getId())) {
            max = Math.max(max, parseTrailingNumber(q.getQuoteNumber(), prefix));
        }
        return max + 1;
    }

    private int firstInvoiceNumberFor(Organization org, String prefix) {
        int max = 1000;
        for (Invoice inv : invoiceRepository.findByOrganizationId(org.getId())) {
            max = Math.max(max, parseTrailingNumber(inv.getInvoiceNumber(), prefix));
        }
        return max + 1;
    }

    private int parseTrailingNumber(String number, String prefix) {
        if (number == null || !number.startsWith(prefix)) return 1000;
        try {
            return Integer.parseInt(number.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return 1000;
        }
    }

    private Organization requireCurrentOrganization() {
        Organization org = currentOrganization();
        if (org == null) {
            throw new IllegalStateException("No organization context — cannot generate a document number.");
        }
        return org;
    }
}
