package com.orque.crm.contact.dto;

import com.orque.crm.enums.ContactStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateContactRequest {

    private ContactStatus status;

    @NotBlank
    private String fullName;

    private String company;

    @NotBlank
    @Email
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
}