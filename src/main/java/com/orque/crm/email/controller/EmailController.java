package com.orque.crm.email.controller;

import com.orque.crm.email.dto.*;
import com.orque.crm.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/mailboxes/connect")
    public ResponseEntity<String> connectMailbox(
            @RequestBody ConnectMailboxRequest request
    ) {
        emailService.connectMailbox(request);
        return ResponseEntity.ok("Mailbox connected successfully");
    }

    @PostMapping("/templates")
    public ResponseEntity<EmailTemplateResponse> createTemplate(
            @RequestBody EmailTemplateRequest request
    ) {
        return ResponseEntity.ok(
                emailService.createTemplate(request)
        );
    }

    @GetMapping("/templates")
    public ResponseEntity<List<EmailTemplateResponse>> getTemplates() {
        return ResponseEntity.ok(
                emailService.getTemplates()
        );
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(
            @RequestBody SendEmailRequest request
    ) {
        emailService.sendEmail(request);
        return ResponseEntity.ok("Email sent successfully");
    }

    @GetMapping("/lead/{leadId}")
    public ResponseEntity<List<EmailMessageResponse>> getLeadEmails(
            @PathVariable Long leadId
    ) {
        return ResponseEntity.ok(
                emailService.getLeadEmails(leadId)
        );
    }

    @GetMapping("/contact/{contactId}")
    public ResponseEntity<List<EmailMessageResponse>> getContactEmails(
            @PathVariable Long contactId
    ) {
        return ResponseEntity.ok(
                emailService.getContactEmails(contactId)
        );
    }

    @GetMapping("/lead/{leadId}/timeline")
    public ResponseEntity<List<CommunicationHistoryResponse>>
    getLeadTimeline(
            @PathVariable Long leadId
    ) {
        return ResponseEntity.ok(
                emailService.getLeadCommunicationHistory(leadId)
        );
    }

    @GetMapping("/contact/{contactId}/timeline")
    public ResponseEntity<List<CommunicationHistoryResponse>>
    getContactTimeline(
            @PathVariable Long contactId
    ) {
        return ResponseEntity.ok(
                emailService.getContactCommunicationHistory(contactId)
        );
    }
    @GetMapping("/inbox/{mailboxId}")
    public ResponseEntity<List<GmailInboxMessageResponse>> getInboxMessages(
            @PathVariable Long mailboxId
    ) {
        return ResponseEntity.ok(
                emailService.getInboxMessages(mailboxId)
        );
    }
}