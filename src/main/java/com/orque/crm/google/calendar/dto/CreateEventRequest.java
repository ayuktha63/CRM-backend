package com.orque.crm.google.calendar.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateEventRequest {
    private String title;
    private String description;
    private String location;
    private String startDateTime;   // ISO-8601, e.g. 2026-08-01T10:00:00
    private String endDateTime;
    private String timeZone;        // e.g. Asia/Kolkata; defaults to server zone if omitted
    private List<String> attendees; // email addresses
    private String recurrenceRule;  // RRULE string, e.g. "RRULE:FREQ=WEEKLY;COUNT=5"
    private boolean createMeetLink;
}
