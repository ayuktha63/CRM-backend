package com.orque.crm.email.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmailFolderPage {
    private List<EmailMessageResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean hasMore;
}
