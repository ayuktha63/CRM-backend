package com.orque.crm.google.gmail.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GmailMessageSummary {
    private String id;
    private String threadId;
    private String from;
    private String subject;
    private String snippet;
    private String internalDate;
    private boolean unread;
    private boolean hasAttachments;
    private List<String> labelIds;
}
