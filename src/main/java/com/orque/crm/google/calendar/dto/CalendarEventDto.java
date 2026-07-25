package com.orque.crm.google.calendar.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CalendarEventDto {
    private String id;
    private String title;
    private String description;
    private String location;
    private String startDateTime;
    private String endDateTime;
    private String timeZone;
    private boolean allDay;
    private String recurrenceRule;
    private List<String> attendees;

    /** Populated only when this event has a Google Meet conference attached. */
    private String meetLink;
    private String conferenceId;
}
