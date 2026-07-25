package com.orque.crm.google.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.orque.crm.google.gmail.dto.GmailMessageDetail;
import com.orque.crm.google.gmail.dto.GmailMessageSummary;
import com.orque.crm.google.gmail.dto.GmailPage;
import com.orque.crm.google.gmail.dto.SendEmailRequest;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import com.orque.crm.google.token.GoogleTokenManager;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class GmailService {

    private static final String USER_ID = "me";

    private final GoogleTokenManager tokenManager;

    // ── Read ─────────────────────────────────────────────────────────────────

    public GmailPage listInbox(String username, String pageToken, int maxResults) {
        return listByQuery(username, "in:inbox", pageToken, maxResults);
    }

    public GmailPage search(String username, String query, String pageToken, int maxResults) {
        return listByQuery(username, query, pageToken, maxResults);
    }

    private GmailPage listByQuery(String username, String query, String pageToken, int maxResults) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Gmail client = buildClient(credential);
            ListMessagesResponse response = client.users().messages().list(USER_ID)
                    .setQ(query)
                    .setMaxResults((long) Math.min(Math.max(maxResults, 1), 100))
                    .setPageToken(pageToken)
                    .execute();

            List<GmailMessageSummary> summaries = new ArrayList<>();
            if (response.getMessages() != null) {
                for (Message ref : response.getMessages()) {
                    Message full = client.users().messages().get(USER_ID, ref.getId())
                            .setFormat("metadata")
                            .setMetadataHeaders(List.of("From", "Subject"))
                            .execute();
                    summaries.add(toSummary(full));
                }
            }
            tokenManager.recordApiSuccess(username);
            return GmailPage.builder()
                    .messages(summaries)
                    .nextPageToken(response.getNextPageToken())
                    .resultSizeEstimate(response.getResultSizeEstimate())
                    .build();
        } catch (Exception e) {
            log.warn("Gmail list failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to fetch Gmail messages", e);
        }
    }

    public GmailMessageDetail getMessage(String username, String messageId) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Gmail client = buildClient(credential);
            Message message = client.users().messages().get(USER_ID, messageId).setFormat("full").execute();
            tokenManager.recordApiSuccess(username);
            return toDetail(message);
        } catch (Exception e) {
            log.warn("Gmail getMessage failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to fetch Gmail message", e);
        }
    }

    public byte[] getAttachment(String username, String messageId, String attachmentId) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Gmail client = buildClient(credential);
            MessagePartBody body = client.users().messages().attachments()
                    .get(USER_ID, messageId, attachmentId).execute();
            tokenManager.recordApiSuccess(username);
            return Base64.getUrlDecoder().decode(body.getData());
        } catch (Exception e) {
            log.warn("Gmail attachment fetch failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to fetch Gmail attachment", e);
        }
    }

    public List<Label> listLabels(String username) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Gmail client = buildClient(credential);
            ListLabelsResponse response = client.users().labels().list(USER_ID).execute();
            tokenManager.recordApiSuccess(username);
            return response.getLabels();
        } catch (Exception e) {
            log.warn("Gmail listLabels failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to fetch Gmail labels", e);
        }
    }

    // ── Write ────────────────────────────────────────────────────────────────

    public GmailMessageSummary send(String username, SendEmailRequest request) {
        return sendMime(username, buildMime(username, request, null, null), null);
    }

    /** Reply keeps the thread by carrying the parent's threadId and RFC In-Reply-To/References headers. */
    public GmailMessageSummary reply(String username, String messageId, SendEmailRequest request, boolean replyAll) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Gmail client = buildClient(credential);
            Message original = client.users().messages().get(USER_ID, messageId).setFormat("full").execute();
            String threadId = original.getThreadId();
            String rfcMessageId = header(original, "Message-ID");
            String subject = header(original, "Subject");
            if (subject != null && !subject.toLowerCase().startsWith("re:")) subject = "Re: " + subject;

            SendEmailRequest merged = new SendEmailRequest();
            merged.setTo(request.getTo() != null && !request.getTo().isEmpty()
                    ? request.getTo() : List.of(header(original, "Reply-To") != null
                            ? header(original, "Reply-To") : header(original, "From")));
            merged.setCc(replyAll ? request.getCc() : null);
            merged.setBcc(request.getBcc());
            merged.setSubject(subject);
            merged.setBodyHtml(request.getBodyHtml());

            return sendMime(username, buildMime(username, merged, rfcMessageId, rfcMessageId), threadId);
        } catch (Exception e) {
            log.warn("Gmail reply failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to send reply", e);
        }
    }

    public GmailMessageSummary forward(String username, String messageId, SendEmailRequest request) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Gmail client = buildClient(credential);
            Message original = client.users().messages().get(USER_ID, messageId).setFormat("full").execute();
            String subject = header(original, "Subject");
            if (subject != null && !subject.toLowerCase().startsWith("fwd:")) subject = "Fwd: " + subject;

            SendEmailRequest fwd = new SendEmailRequest();
            fwd.setTo(request.getTo());
            fwd.setCc(request.getCc());
            fwd.setBcc(request.getBcc());
            fwd.setSubject(subject);
            fwd.setBodyHtml((request.getBodyHtml() != null ? request.getBodyHtml() : "")
                    + "<br><br>---------- Forwarded message ----------<br>" + extractHtmlOrText(original));

            return sendMime(username, buildMime(username, fwd, null, null), null);
        } catch (Exception e) {
            log.warn("Gmail forward failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to forward message", e);
        }
    }

    public GmailMessageSummary saveDraft(String username, SendEmailRequest request) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Gmail client = buildClient(credential);
            Message mime = buildMime(username, request, null, null);
            Draft draft = new Draft().setMessage(mime);
            Draft created = client.users().drafts().create(USER_ID, draft).execute();
            tokenManager.recordApiSuccess(username);
            return toSummary(created.getMessage());
        } catch (Exception e) {
            log.warn("Gmail saveDraft failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to save draft", e);
        }
    }

    /** Soft delete — moves the message to Trash, recoverable via {@link #untrashMessage}. */
    public void deleteMessage(String username, String messageId) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            buildClient(credential).users().messages().trash(USER_ID, messageId).execute();
            tokenManager.recordApiSuccess(username);
        } catch (Exception e) {
            log.warn("Gmail delete failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to delete message", e);
        }
    }

    /** Restores a trashed message back to its previous labels (e.g. Inbox). */
    public void untrashMessage(String username, String messageId) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            buildClient(credential).users().messages().untrash(USER_ID, messageId).execute();
            tokenManager.recordApiSuccess(username);
        } catch (Exception e) {
            log.warn("Gmail untrash failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to restore message", e);
        }
    }

    /** Hard delete — permanent, unrecoverable. Only ever offered from the Trash view. */
    public void permanentlyDeleteMessage(String username, String messageId) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            buildClient(credential).users().messages().delete(USER_ID, messageId).execute();
            tokenManager.recordApiSuccess(username);
        } catch (Exception e) {
            log.warn("Gmail permanent delete failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to permanently delete message", e);
        }
    }

    public void archiveMessage(String username, String messageId) {
        modifyLabels(username, messageId, null, List.of("INBOX"));
    }

    public void applyLabel(String username, String messageId, String labelId) {
        modifyLabels(username, messageId, List.of(labelId), null);
    }

    public void removeLabel(String username, String messageId, String labelId) {
        modifyLabels(username, messageId, null, List.of(labelId));
    }

    private void modifyLabels(String username, String messageId, List<String> add, List<String> remove) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            ModifyMessageRequest req = new ModifyMessageRequest();
            if (add != null) req.setAddLabelIds(add);
            if (remove != null) req.setRemoveLabelIds(remove);
            buildClient(credential).users().messages().modify(USER_ID, messageId, req).execute();
            tokenManager.recordApiSuccess(username);
        } catch (Exception e) {
            log.warn("Gmail label update failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to update labels", e);
        }
    }

    // ── Internals ──────────────────────────────────────────────────────────

    private GmailMessageSummary sendMime(String username, Message mime, String threadId) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            if (threadId != null) mime.setThreadId(threadId);
            Gmail client = buildClient(credential);
            Message sent = client.users().messages().send(USER_ID, mime).execute();
            tokenManager.recordApiSuccess(username);
            return toSummary(sent);
        } catch (Exception e) {
            log.warn("Gmail send failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to send email", e);
        }
    }

    private Message buildMime(String username, SendEmailRequest request, String inReplyTo, String references) {
        try {
            GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
            Session session = Session.getDefaultInstance(new Properties(), null);
            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress(credential.getEmail()));
            for (String to : request.getTo()) email.addRecipient(RecipientType.TO, new InternetAddress(to));
            if (request.getCc() != null) for (String cc : request.getCc()) email.addRecipient(RecipientType.CC, new InternetAddress(cc));
            if (request.getBcc() != null) for (String bcc : request.getBcc()) email.addRecipient(RecipientType.BCC, new InternetAddress(bcc));
            email.setSubject(request.getSubject());
            email.setContent(request.getBodyHtml() != null ? request.getBodyHtml() : "", "text/html; charset=utf-8");
            if (inReplyTo != null) email.addHeader("In-Reply-To", inReplyTo);
            if (references != null) email.addHeader("References", references);

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            String encoded = Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
            return new Message().setRaw(encoded);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build outgoing email", e);
        }
    }

    private Gmail buildClient(GoogleWorkspaceCredential credential) throws Exception {
        return new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
                tokenManager.buildCredential(credential))
                .setApplicationName("Orque CRM")
                .build();
    }

    private GmailMessageSummary toSummary(Message message) {
        String from = header(message, "From");
        String subject = header(message, "Subject");
        List<String> labelIds = message.getLabelIds();
        return GmailMessageSummary.builder()
                .id(message.getId())
                .threadId(message.getThreadId())
                .from(from)
                .subject(subject)
                .snippet(message.getSnippet())
                .internalDate(message.getInternalDate() != null ? message.getInternalDate().toString() : null)
                .unread(labelIds != null && labelIds.contains("UNREAD"))
                .hasAttachments(hasAttachments(message))
                .labelIds(labelIds)
                .build();
    }

    private GmailMessageDetail toDetail(Message message) {
        List<GmailMessageDetail.Attachment> attachments = new ArrayList<>();
        collectAttachments(message.getPayload(), attachments);

        return GmailMessageDetail.builder()
                .id(message.getId())
                .threadId(message.getThreadId())
                .from(header(message, "From"))
                .to(header(message, "To"))
                .cc(header(message, "Cc"))
                .subject(header(message, "Subject"))
                .bodyHtml(extractBody(message.getPayload(), "text/html"))
                .bodyText(extractBody(message.getPayload(), "text/plain"))
                .internalDate(message.getInternalDate() != null ? message.getInternalDate().toString() : null)
                .labelIds(message.getLabelIds())
                .attachments(attachments)
                .build();
    }

    private String header(Message message, String name) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) return null;
        return message.getPayload().getHeaders().stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst().orElse(null);
    }

    private boolean hasAttachments(Message message) {
        if (message.getPayload() == null || message.getPayload().getParts() == null) return false;
        return message.getPayload().getParts().stream()
                .anyMatch(p -> p.getFilename() != null && !p.getFilename().isBlank());
    }

    private void collectAttachments(MessagePart part, List<GmailMessageDetail.Attachment> out) {
        if (part == null) return;
        if (part.getFilename() != null && !part.getFilename().isBlank() && part.getBody() != null) {
            out.add(GmailMessageDetail.Attachment.builder()
                    .attachmentId(part.getBody().getAttachmentId())
                    .filename(part.getFilename())
                    .mimeType(part.getMimeType())
                    .size(part.getBody().getSize() != null ? part.getBody().getSize().longValue() : null)
                    .build());
        }
        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) collectAttachments(child, out);
        }
    }

    private String extractBody(MessagePart part, String mimeType) {
        if (part == null) return null;
        if (mimeType.equals(part.getMimeType()) && part.getBody() != null && part.getBody().getData() != null) {
            return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
        }
        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                String found = extractBody(child, mimeType);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String extractHtmlOrText(Message message) {
        String html = extractBody(message.getPayload(), "text/html");
        return html != null ? html : (extractBody(message.getPayload(), "text/plain") != null
                ? extractBody(message.getPayload(), "text/plain") : "");
    }
}
