package com.orque.crm.email.service;

import com.orque.crm.email.dto.*;

import java.util.List;

public interface EmailService {

    void connectMailbox(ConnectMailboxRequest request);

    EmailTemplateResponse createTemplate(EmailTemplateRequest request);

    List<EmailTemplateResponse> getTemplates();

    void sendEmail(SendEmailRequest request);

    List<EmailMessageResponse> getLeadEmails(Long leadId);

    List<EmailMessageResponse> getContactEmails(Long contactId);

    List<CommunicationHistoryResponse> getLeadCommunicationHistory(Long leadId);

    List<CommunicationHistoryResponse> getContactCommunicationHistory(Long contactId);

    List<GmailInboxMessageResponse> getInboxMessages(Long mailboxId);

    List<EmailMessageResponse> getLogsForCurrentUser();
}