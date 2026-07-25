package com.orque.crm.google.gmail.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GmailMessageDetail {
    private String id;
    private String threadId;
    private String from;
    private String to;
    private String cc;
    private String subject;
    private String bodyHtml;
    private String bodyText;
    private String internalDate;
    private List<String> labelIds;
    private List<Attachment> attachments;

    @Data
    @Builder
    public static class Attachment {
        private String attachmentId;
        private String filename;
        private String mimeType;
        private Long size;
    }
}
