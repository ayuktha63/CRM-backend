package com.orque.crm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum LeadSource {

    MANUAL,
    CONTACT_CONVERSION,
    CSV_IMPORT,
    WEBSITE,
    REFERRAL,
    OTHER;

    @JsonCreator
    public static LeadSource fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        try {
            return LeadSource.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            for (LeadSource source : LeadSource.values()) {
                if (source.name().equalsIgnoreCase(normalized)) {
                    return source;
                }
            }
            return LeadSource.OTHER;
        }
    }
}