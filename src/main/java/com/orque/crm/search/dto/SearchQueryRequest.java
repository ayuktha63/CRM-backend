package com.orque.crm.search.dto;

import lombok.Data;

/** Body payload for QUERY /api/v1/search (RFC 10008). */
@Data
public class SearchQueryRequest {
    /** Full-text search term — minimum 2 characters. */
    private String q;
}
