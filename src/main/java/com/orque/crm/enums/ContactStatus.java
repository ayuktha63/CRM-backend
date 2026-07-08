package com.orque.crm.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ContactStatus {
    NEW,
    REVIEWED,
    CONVERTED_TO_LEAD;

    @JsonCreator
    public static ContactStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NEW;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        try {
            return ContactStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            for (ContactStatus status : ContactStatus.values()) {
                if (status.name().equalsIgnoreCase(normalized)) {
                    return status;
                }
            }
            return NEW;
        }
    }
}