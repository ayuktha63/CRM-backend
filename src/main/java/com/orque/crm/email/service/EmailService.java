package com.orque.crm.email.service;

import com.orque.crm.email.dto.*;

import java.util.List;

public interface EmailService {

    void connectMailbox(ConnectMailboxRequest request);

    List<MailboxResponse> listMailboxes();

    void deleteMailbox(Long id);

    EmailTemplateResponse createTemplate(EmailTemplateRequest request);

    List<EmailTemplateResponse> getTemplates();

    void sendEmail(SendEmailRequest request);

    List<EmailMessageResponse> getLeadEmails(Long leadId);

    List<EmailMessageResponse> getContactEmails(Long contactId);

    List<CommunicationHistoryResponse> getLeadCommunicationHistory(Long leadId);

    List<CommunicationHistoryResponse> getContactCommunicationHistory(Long contactId);

    List<GmailInboxMessageResponse> getInboxMessages(Long mailboxId);

    List<EmailMessageResponse> getLogsForCurrentUser();

    EmailFolderPage getEmailsByFolder(String folderName, int page, int size);

    void updateEmailFolder(Long id, String folderName);

    void toggleEmailStar(Long id);

    void recordEmailOpen(Long id);

    void recordEmailClick(Long id);

    void markEmailRead(Long id, boolean read);

    void deleteEmailPermanently(Long id);
}