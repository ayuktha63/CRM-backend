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

    @GetMapping("/mailboxes")
    public ResponseEntity<List<com.orque.crm.email.dto.MailboxResponse>> listMailboxes() {
        return ResponseEntity.ok(emailService.listMailboxes());
    }

    @DeleteMapping("/mailboxes/{id}")
    public ResponseEntity<Void> deleteMailbox(@PathVariable Long id) {
        emailService.deleteMailbox(id);
        return ResponseEntity.noContent().build();
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

    @GetMapping("/logs")
    public ResponseEntity<List<EmailMessageResponse>> getMyLogs() {
        return ResponseEntity.ok(emailService.getLogsForCurrentUser());
    }

    @GetMapping("/folder/{folderName}")
    public ResponseEntity<com.orque.crm.email.dto.EmailFolderPage> getEmailsByFolder(
            @PathVariable String folderName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(emailService.getEmailsByFolder(folderName, page, size));
    }

    @PutMapping("/{id}/folder")
    public ResponseEntity<Void> updateEmailFolder(@PathVariable Long id, @RequestParam String folder) {
        emailService.updateEmailFolder(id, folder);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/star")
    public ResponseEntity<Void> toggleEmailStar(@PathVariable Long id) {
        emailService.toggleEmailStar(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tracker/open/{id}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable Long id) {
        emailService.recordEmailOpen(id);
        byte[] gif = java.util.Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.IMAGE_GIF)
                .body(gif);
    }

    @GetMapping("/tracker/click/{id}")
    public ResponseEntity<Void> trackClick(@PathVariable Long id, @RequestParam String redirectUrl) {
        emailService.recordEmailClick(id);
        return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                .header(org.springframework.http.HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @RequestParam boolean read) {
        emailService.markEmailRead(id, read);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmailPermanently(@PathVariable Long id) {
        emailService.deleteEmailPermanently(id);
        return ResponseEntity.noContent().build();
    }
}