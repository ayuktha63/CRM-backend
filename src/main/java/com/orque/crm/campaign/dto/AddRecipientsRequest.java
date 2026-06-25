package com.orque.crm.campaign.dto;

import lombok.Data;

@Data
public class AddRecipientsRequest {

    private Long contactId;

    private Long leadId;

    private String firstName;

    private String lastName;

    private String company;

    private String email;
}