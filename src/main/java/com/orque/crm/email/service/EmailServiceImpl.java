
package com.orque.crm.email.service;

import com.orque.crm.email.dto.*;
import com.orque.crm.email.entity.*;
import com.orque.crm.email.repository.*;
import com.orque.crm.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.orque.crm.audit.service.AuditLogService;
import com.orque.crm.enums.AuditAction;
import com.orque.crm.enums.AuditModule;
import java.util.ArrayList;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final ConnectedMailboxRepository connectedMailboxRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final CommunicationHistoryRepository communicationHistoryRepository;
    private final AuditLogService auditLogService;
    private final com.orque.crm.settings.repository.UserSettingsRepository userSettingsRepository;

    @Override
    public void connectMailbox(ConnectMailboxRequest request) {

        ConnectedMailbox mailbox = ConnectedMailbox.builder()
                .emailAddress(request.getEmailAddress())
                .provider(request.getProvider())
                .status(MailboxStatus.CONNECTED)
                .connectedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        connectedMailboxRepository.save(mailbox);
    }

    @Override
    public EmailTemplateResponse createTemplate(EmailTemplateRequest request) {

        EmailTemplate template = EmailTemplate.builder()
                .templateName(request.getTemplateName())
                .subject(request.getSubject())
                .body(request.getBody())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        EmailTemplate saved = emailTemplateRepository.save(template);

        return mapTemplateToResponse(saved);
    }

    @Override
    public List<EmailTemplateResponse> getTemplates() {
        return emailTemplateRepository.findAll()
                .stream()
                .map(this::mapTemplateToResponse)
                .toList();
    }

    @Override
    public void sendEmail(SendEmailRequest request) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {}

        com.orque.crm.settings.entity.UserSettings settings = userSettingsRepository.findByUsername(username).orElse(null);

        String fromEmail = "system@orque.com";
        Long mailboxId = request.getMailboxId();
        if (settings != null && settings.getMailUsername() != null && !settings.getMailUsername().trim().isEmpty()) {
            fromEmail = settings.getMailUsername();
        } else {
            ConnectedMailbox mailbox = connectedMailboxRepository.findById(request.getMailboxId() != null ? request.getMailboxId() : -1L).orElse(null);
            if (mailbox != null) {
                fromEmail = mailbox.getEmailAddress();
                mailboxId = mailbox.getId();
            }
        }

        boolean isDraft = Boolean.TRUE.equals(request.getIsDraft());
        boolean isScheduled = request.getScheduledAt() != null && !request.getScheduledAt().trim().isEmpty();

        EmailMessageStatus status = EmailMessageStatus.SENT;
        String folder = "SENT";
        LocalDateTime scheduledTime = null;
        LocalDateTime sentTime = LocalDateTime.now();

        if (isDraft) {
            status = EmailMessageStatus.DRAFT;
            folder = "DRAFT";
            sentTime = null;
        } else if (isScheduled) {
            status = EmailMessageStatus.SCHEDULED;
            folder = "SCHEDULED";
            try {
                scheduledTime = LocalDateTime.parse(request.getScheduledAt());
            } catch (Exception e) {
                scheduledTime = LocalDateTime.now().plusHours(1);
            }
            sentTime = null;
        }

        // Try to send real email using Gmail SMTP and App Password if configured
        if (!isDraft && !isScheduled && settings != null && settings.getMailHost() != null && settings.getMailUsername() != null && settings.getMailPassword() != null) {
            try {
                sendRealEmail(settings, request.getToEmail(), request.getCc(), request.getBcc(), request.getSubject(), request.getBody());
            } catch (Exception e) {
                System.err.println("SMTP real email send failed: " + e.getMessage());
            }
        }

        EmailMessage emailMessage = EmailMessage.builder()
                .mailboxId(mailboxId)
                .contactId(request.getContactId())
                .leadId(request.getLeadId())
                .fromEmail(fromEmail)
                .toEmail(request.getToEmail())
                .cc(request.getCc())
                .bcc(request.getBcc())
                .subject(request.getSubject())
                .body(request.getBody())
                .direction(EmailDirection.OUTBOUND)
                .status(status)
                .folder(folder)
                .isDraft(isDraft)
                .scheduledAt(scheduledTime)
                .sentAt(sentTime)
                .createdAt(LocalDateTime.now())
                .build();

        EmailMessage savedEmail = emailMessageRepository.save(emailMessage);

        if (!isDraft) {
            CommunicationHistory history = CommunicationHistory.builder()
                    .contactId(request.getContactId())
                    .leadId(request.getLeadId())
                    .emailMessageId(savedEmail.getId())
                    .activityType(isScheduled ? EmailActivityType.EMAIL_SENT : EmailActivityType.EMAIL_SENT)
                    .description(isScheduled ? "Email scheduled for " + request.getToEmail() : "Email sent to " + request.getToEmail())
                    .activityAt(LocalDateTime.now())
                    .build();

            communicationHistoryRepository.save(history);
            auditLogService.createAudit(
                    isScheduled ? AuditAction.valueOf("EMAIL_SENT") : AuditAction.EMAIL_SENT,
                    AuditModule.EMAIL,
                    "Email",
                    savedEmail.getId(),
                    null,
                    request.getToEmail(),
                    isScheduled ? "Email scheduled to " + request.getToEmail() : "Email sent to " + request.getToEmail(),
                    fromEmail,
                    null
            );
        }
    }

    private void sendRealEmail(com.orque.crm.settings.entity.UserSettings settings, String to, String cc, String bcc, String subject, String body) {
        java.util.Properties props = new java.util.Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", settings.getMailHost() != null ? settings.getMailHost() : "smtp.gmail.com");
        props.put("mail.smtp.port", settings.getMailPort() != null ? String.valueOf(settings.getMailPort()) : "587");

        jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(settings.getMailUsername(), settings.getMailPassword());
            }
        });

        try {
            jakarta.mail.Message message = new jakarta.mail.internet.MimeMessage(session);
            message.setFrom(new jakarta.mail.internet.InternetAddress(settings.getMailFromAddress() != null && !settings.getMailFromAddress().trim().isEmpty() ? settings.getMailFromAddress() : settings.getMailUsername()));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO, jakarta.mail.internet.InternetAddress.parse(to));
            if (cc != null && !cc.trim().isEmpty()) {
                message.setRecipients(jakarta.mail.Message.RecipientType.CC, jakarta.mail.internet.InternetAddress.parse(cc));
            }
            if (bcc != null && !bcc.trim().isEmpty()) {
                message.setRecipients(jakarta.mail.Message.RecipientType.BCC, jakarta.mail.internet.InternetAddress.parse(bcc));
            }
            message.setSubject(subject);
            message.setText(body);

            jakarta.mail.Transport.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMTP email: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EmailMessageResponse> getLeadEmails(Long leadId) {
        return emailMessageRepository.findByLeadId(leadId)
                .stream()
                .map(this::mapEmailToResponse)
                .toList();
    }

    @Override
    public List<EmailMessageResponse> getContactEmails(Long contactId) {
        return emailMessageRepository.findByContactId(contactId)
                .stream()
                .map(this::mapEmailToResponse)
                .toList();
    }

    @Override
    public List<CommunicationHistoryResponse> getLeadCommunicationHistory(Long leadId) {
        return communicationHistoryRepository.findByLeadIdOrderByActivityAtDesc(leadId)
                .stream()
                .map(this::mapHistoryToResponse)
                .toList();
    }

    @Override
    public List<CommunicationHistoryResponse> getContactCommunicationHistory(Long contactId) {
        return communicationHistoryRepository.findByContactIdOrderByActivityAtDesc(contactId)
                .stream()
                .map(this::mapHistoryToResponse)
                .toList();
    }

    private EmailTemplateResponse mapTemplateToResponse(EmailTemplate template) {
        return EmailTemplateResponse.builder()
                .id(template.getId())
                .templateName(template.getTemplateName())
                .subject(template.getSubject())
                .body(template.getBody())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private EmailMessageResponse mapEmailToResponse(EmailMessage emailMessage) {
        return EmailMessageResponse.builder()
                .id(emailMessage.getId())
                .contactId(emailMessage.getContactId())
                .leadId(emailMessage.getLeadId())
                .fromEmail(emailMessage.getFromEmail())
                .toEmail(emailMessage.getToEmail())
                .subject(emailMessage.getSubject())
                .body(emailMessage.getBody())
                .gmailMessageId(emailMessage.getGmailMessageId())
                .gmailThreadId(emailMessage.getGmailThreadId())
                .direction(emailMessage.getDirection())
                .status(emailMessage.getStatus())
                .sentAt(emailMessage.getSentAt())
                .receivedAt(emailMessage.getReceivedAt())
                .folder(emailMessage.getFolder() != null ? emailMessage.getFolder() : "INBOX")
                .isStarred(emailMessage.getIsStarred() != null && emailMessage.getIsStarred())
                .isPinned(emailMessage.getIsPinned() != null && emailMessage.getIsPinned())
                .openCount(emailMessage.getOpenCount() != null ? emailMessage.getOpenCount() : 0)
                .clickCount(emailMessage.getClickCount() != null ? emailMessage.getClickCount() : 0)
                .bounceReason(emailMessage.getBounceReason())
                .cc(emailMessage.getCc())
                .bcc(emailMessage.getBcc())
                .scheduledAt(emailMessage.getScheduledAt())
                .isDraft(emailMessage.getIsDraft())
                .build();
    }

    private CommunicationHistoryResponse mapHistoryToResponse(CommunicationHistory history) {
        return CommunicationHistoryResponse.builder()
                .id(history.getId())
                .contactId(history.getContactId())
                .leadId(history.getLeadId())
                .emailMessageId(history.getEmailMessageId())
                .activityType(history.getActivityType())
                .description(history.getDescription())
                .activityAt(history.getActivityAt())
                .build();
    }
    @Override
    public List<GmailInboxMessageResponse> getInboxMessages(Long mailboxId) {

        ConnectedMailbox mailbox = connectedMailboxRepository.findById(mailboxId)
                .orElseThrow(() -> new RuntimeException("Mailbox not found"));

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(mailbox.getAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String listUrl =
                "https://gmail.googleapis.com/gmail/v1/users/me/messages"
                        + "?maxResults=10";

        ResponseEntity<Map> listResponse = restTemplate.exchange(
                listUrl,
                HttpMethod.GET,
                entity,
                Map.class
        );

        List<Map<String, String>> messages =
                (List<Map<String, String>>) listResponse.getBody().get("messages");

        List<GmailInboxMessageResponse> inboxMessages = new ArrayList<>();

        if (messages == null) {
            return inboxMessages;
        }

        for (Map<String, String> message : messages) {

            String messageId = message.get("id");

            String messageUrl =
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages/"
                            + messageId
                            + "?format=metadata";

            ResponseEntity<Map> messageResponse = restTemplate.exchange(
                    messageUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> messageBody = messageResponse.getBody();

            String threadId = (String) messageBody.get("threadId");
            String snippet = (String) messageBody.get("snippet");

            Map<String, Object> payload =
                    (Map<String, Object>) messageBody.get("payload");

            List<Map<String, Object>> headersList =
                    (List<Map<String, Object>>) payload.get("headers");

            String fromEmail = null;
            String toEmail = null;
            String subject = null;

            for (Map<String, Object> header : headersList) {

                String name = (String) header.get("name");
                String value = (String) header.get("value");

                if ("From".equalsIgnoreCase(name)) {
                    fromEmail = value;
                }

                if ("To".equalsIgnoreCase(name)) {
                    toEmail = value;
                }

                if ("Subject".equalsIgnoreCase(name)) {
                    subject = value;
                }
            }

            inboxMessages.add(
                    GmailInboxMessageResponse.builder()
                            .gmailMessageId(messageId)
                            .gmailThreadId(threadId)
                            .fromEmail(fromEmail)
                            .toEmail(toEmail)
                            .subject(subject)
                            .snippet(snippet)
                            .build()
            );
        }

        return inboxMessages;
    }

    @Override
    public List<EmailMessageResponse> getLogsForCurrentUser() {
        String email = "";
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.orque.crm.auth.entity.User u) {
            email = u.getEmail();
        } else {
            email = "admin@orque.com";
        }
        return emailMessageRepository.findByFromEmailOrderBySentAtDesc(email)
                .stream()
                .map(this::mapEmailToResponse)
                .toList();
    }

    @Override
    public List<EmailMessageResponse> getEmailsByFolder(String folderName) {
        if ("INBOX".equalsIgnoreCase(folderName)) {
            String username = "system";
            try {
                username = com.orque.crm.common.UserContextHelper.currentUsername();
            } catch (Exception e) {}
            com.orque.crm.settings.entity.UserSettings settings = userSettingsRepository.findByUsername(username).orElse(null);
            if (settings != null && settings.getMailUsername() != null && settings.getMailPassword() != null) {
                syncImapInbox(settings);
            }
        }
        return emailMessageRepository.findByFolderOrderBySentAtDesc(folderName.toUpperCase())
                .stream()
                .map(this::mapEmailToResponse)
                .toList();
    }

    private void syncImapInbox(com.orque.crm.settings.entity.UserSettings settings) {
        if (settings.getMailUsername() == null || settings.getMailPassword() == null) {
            return;
        }
        try {
            java.util.Properties props = new java.util.Properties();
            props.put("mail.store.protocol", "imaps");
            props.put("mail.imaps.host", "imap.gmail.com");
            props.put("mail.imaps.port", "993");
            props.put("mail.imaps.ssl.enable", "true");

            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props);
            jakarta.mail.Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", settings.getMailUsername(), settings.getMailPassword());

            jakarta.mail.Folder inbox = store.getFolder("INBOX");
            inbox.open(jakarta.mail.Folder.READ_ONLY);

            int count = inbox.getMessageCount();
            int start = Math.max(1, count - 19);
            jakarta.mail.Message[] messages = inbox.getMessages(start, count);

            for (jakarta.mail.Message msg : messages) {
                String from = "";
                if (msg.getFrom() != null && msg.getFrom().length > 0) {
                    from = msg.getFrom()[0].toString();
                }
                String subject = msg.getSubject();
                
                List<EmailMessage> existing = emailMessageRepository.findByFromEmailOrderBySentAtDesc(from);
                boolean exists = existing.stream().anyMatch(e -> subject.equals(e.getSubject()));
                if (exists) {
                    continue;
                }

                String body = "";
                try {
                    Object content = msg.getContent();
                    if (content instanceof String) {
                        body = (String) content;
                    } else if (content instanceof jakarta.mail.internet.MimeMultipart) {
                        jakarta.mail.internet.MimeMultipart mp = (jakarta.mail.internet.MimeMultipart) content;
                        if (mp.getCount() > 0) {
                            body = mp.getBodyPart(0).getContent().toString();
                        }
                    }
                } catch (Exception ex) {
                    body = "[HTML Content or Attachments]";
                }

                EmailMessage emailMessage = EmailMessage.builder()
                        .fromEmail(from)
                        .toEmail(settings.getMailUsername())
                        .subject(subject)
                        .body(body)
                        .direction(EmailDirection.INBOUND)
                        .status(EmailMessageStatus.SENT)
                        .folder("INBOX")
                        .isDraft(false)
                        .sentAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build();

                emailMessageRepository.save(emailMessage);
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println("IMAP sync failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateEmailFolder(Long id, String folderName) {
        EmailMessage email = emailMessageRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Email not found"));
        email.setFolder(folderName.toUpperCase());
        emailMessageRepository.save(email);
    }

    @Override
    @Transactional
    public void toggleEmailStar(Long id) {
        EmailMessage email = emailMessageRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Email not found"));
        email.setIsStarred(email.getIsStarred() == null ? true : !email.getIsStarred());
        emailMessageRepository.save(email);
    }

    @Override
    @Transactional
    public void recordEmailOpen(Long id) {
        EmailMessage email = emailMessageRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Email not found"));
        email.setOpenCount(email.getOpenCount() == null ? 1 : email.getOpenCount() + 1);
        emailMessageRepository.save(email);
    }

    @Override
    @Transactional
    public void recordEmailClick(Long id) {
        EmailMessage email = emailMessageRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Email not found"));
        email.setClickCount(email.getClickCount() == null ? 1 : email.getClickCount() + 1);
        emailMessageRepository.save(email);
    }
}