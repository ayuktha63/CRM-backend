package com.orque.crm.contact.dto;

import lombok.Data;

/** Body payload for QUERY /api/v1/contacts/search (RFC 10008). */
@Data
public class ContactSearchRequest {
    private String keyword;
}
