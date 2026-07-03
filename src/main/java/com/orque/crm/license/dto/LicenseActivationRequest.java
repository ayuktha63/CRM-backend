package com.orque.crm.license.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LicenseActivationRequest {

    @NotBlank
    private String organizationId;

    @NotBlank
    private String licenseKey;

    private String licenseName;
}
