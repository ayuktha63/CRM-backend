package com.orque.crm.google.gmail;

import com.google.api.services.gmail.model.Label;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.google.gmail.dto.GmailMessageDetail;
import com.orque.crm.google.gmail.dto.GmailMessageSummary;
import com.orque.crm.google.gmail.dto.GmailPage;
import com.orque.crm.google.gmail.dto.SendEmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint here fetches live from Gmail on each call — there is no "Sync Now" step and
 * no dependency on previously cached rows for what gets displayed. Background caching (if added
 * later) is purely a performance optimization, never the source of truth.
 */
@RestController
@RequestMapping("/api/v1/google/gmail")
@RequiredArgsConstructor
public class GmailController {

    private final GmailService gmailService;

    @GetMapping("/inbox")
    public ResponseEntity<GmailPage> inbox(
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "20") int maxResults) {
        return ResponseEntity.ok(gmailService.listInbox(UserContextHelper.currentUsername(), pageToken, maxResults));
    }

    @GetMapping("/search")
    public ResponseEntity<GmailPage> search(
            @RequestParam String q,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "20") int maxResults) {
        return ResponseEntity.ok(gmailService.search(UserContextHelper.currentUsername(), q, pageToken, maxResults));
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<GmailMessageDetail> getMessage(@PathVariable String id) {
        return ResponseEntity.ok(gmailService.getMessage(UserContextHelper.currentUsername(), id));
    }

    @GetMapping("/messages/{messageId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> getAttachment(@PathVariable String messageId, @PathVariable String attachmentId) {
        byte[] bytes = gmailService.getAttachment(UserContextHelper.currentUsername(), messageId, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .body(bytes);
    }

    @GetMapping("/labels")
    public ResponseEntity<List<Label>> labels() {
        return ResponseEntity.ok(gmailService.listLabels(UserContextHelper.currentUsername()));
    }

    @PostMapping("/send")
    public ResponseEntity<GmailMessageSummary> send(@RequestBody SendEmailRequest request) {
        return ResponseEntity.ok(gmailService.send(UserContextHelper.currentUsername(), request));
    }

    @PostMapping("/messages/{id}/reply")
    public ResponseEntity<GmailMessageSummary> reply(@PathVariable String id, @RequestBody SendEmailRequest request) {
        return ResponseEntity.ok(gmailService.reply(UserContextHelper.currentUsername(), id, request, false));
    }

    @PostMapping("/messages/{id}/reply-all")
    public ResponseEntity<GmailMessageSummary> replyAll(@PathVariable String id, @RequestBody SendEmailRequest request) {
        return ResponseEntity.ok(gmailService.reply(UserContextHelper.currentUsername(), id, request, true));
    }

    @PostMapping("/messages/{id}/forward")
    public ResponseEntity<GmailMessageSummary> forward(@PathVariable String id, @RequestBody SendEmailRequest request) {
        return ResponseEntity.ok(gmailService.forward(UserContextHelper.currentUsername(), id, request));
    }

    @PostMapping("/drafts")
    public ResponseEntity<GmailMessageSummary> saveDraft(@RequestBody SendEmailRequest request) {
        return ResponseEntity.ok(gmailService.saveDraft(UserContextHelper.currentUsername(), request));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        gmailService.deleteMessage(UserContextHelper.currentUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages/{id}/untrash")
    public ResponseEntity<Void> untrash(@PathVariable String id) {
        gmailService.untrashMessage(UserContextHelper.currentUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{id}/permanent")
    public ResponseEntity<Void> permanentDelete(@PathVariable String id) {
        gmailService.permanentlyDeleteMessage(UserContextHelper.currentUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable String id) {
        gmailService.archiveMessage(UserContextHelper.currentUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/messages/{id}/labels/{labelId}")
    public ResponseEntity<Void> applyLabel(@PathVariable String id, @PathVariable String labelId) {
        gmailService.applyLabel(UserContextHelper.currentUsername(), id, labelId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{id}/labels/{labelId}")
    public ResponseEntity<Void> removeLabel(@PathVariable String id, @PathVariable String labelId) {
        gmailService.removeLabel(UserContextHelper.currentUsername(), id, labelId);
        return ResponseEntity.noContent().build();
    }
}
