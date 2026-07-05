package com.orque.crm.tax.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaxSettingsResponse {
    private String organizationId;
    private String countryCode;
    private String countryName;
    private String taxSystem;
    private String registrationLabel;
    private String registrationNumber;
    private String businessState;
    private Boolean requiresBusinessState;
    private String configJson;
    private Boolean enabled;
}
