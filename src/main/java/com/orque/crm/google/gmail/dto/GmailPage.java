package com.orque.crm.google.gmail.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GmailPage {
    private List<GmailMessageSummary> messages;
    private String nextPageToken;
    private Long resultSizeEstimate;
}
