package com.orque.crm.contact.dto;

import com.orque.crm.enums.ContactStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ContactResponse {

    private Long id;
    private String fullName;
    private String company;
    private String email;
    private String phone;
    private String jobTitle;
    private String industry;
    private String website;
    private String address;
    private String country;
    private String state;
    private String city;
    private String tags;
    private String notes;
    private ContactStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}