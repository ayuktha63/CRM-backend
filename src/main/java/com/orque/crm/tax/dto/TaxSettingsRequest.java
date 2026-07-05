package com.orque.crm.tax.dto;

import lombok.Data;

@Data
public class TaxSettingsRequest {
    private String countryCode;
    private String countryName;
    private String taxSystem;
    private String registrationLabel;
    private String registrationNumber;
    private String businessState;
    private Boolean requiresBusinessState;
    /** Full rule-set JSON for the chosen country, copied from the frontend's static list. */
    private String configJson;
    private Boolean enabled;
}
