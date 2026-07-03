package com.orque.crm.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationRequest {

    @NotBlank
    private String organizationCode;

    @NotBlank
    private String organizationName;

    private String legalName;

    @Email
    private String email;

    private String phone;
    private String address;
    private String country;
    private String timezone;
    private String currency;
}
