package com.orque.crm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum LeadStatus {

    NEW,
    QUALIFIED,
    CONVERTED,
    DISQUALIFIED;

    @JsonCreator
    public static LeadStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NEW;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        try {
            return LeadStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            for (LeadStatus status : LeadStatus.values()) {
                if (status.name().equalsIgnoreCase(normalized)) {
                    return status;
                }
            }
            return NEW;
        }
    }
}