
package com.orque.crm.email.service;

import com.orque.crm.email.dto.*;
import com.orque.crm.email.entity.*;
import com.orque.crm.email.repository.*;
import com.orque.crm.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

        ConnectedMailbox mailbox = connectedMailboxRepository.findById(request.getMailboxId())
                .orElseThrow(() -> new RuntimeException("Mailbox not found"));

        EmailMessage emailMessage = EmailMessage.builder()
                .mailboxId(mailbox.getId())
                .contactId(request.getContactId())
                .leadId(request.getLeadId())
                .fromEmail(mailbox.getEmailAddress())
                .toEmail(request.getToEmail())
                .subject(request.getSubject())
                .body(request.getBody())
                .direction(EmailDirection.OUTBOUND)
                .status(EmailMessageStatus.SENT)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        EmailMessage savedEmail = emailMessageRepository.save(emailMessage);

        CommunicationHistory history = CommunicationHistory.builder()
                .contactId(request.getContactId())
                .leadId(request.getLeadId())
                .emailMessageId(savedEmail.getId())
                .activityType(EmailActivityType.EMAIL_SENT)
                .description("Email sent to " + request.getToEmail())
                .activityAt(LocalDateTime.now())
                .build();

        communicationHistoryRepository.save(history);
        auditLogService.createAudit(
                AuditAction.EMAIL_SENT,
                AuditModule.EMAIL,
                "Email",
                savedEmail.getId(),
                null,
                request.getToEmail(),
                "Email sent to " + request.getToEmail(),
                mailbox.getEmailAddress(),
                null
        );
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
}