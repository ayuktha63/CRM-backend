package com.orque.crm.google.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.orque.crm.google.calendar.dto.CalendarEventDto;
import com.orque.crm.google.calendar.dto.CreateEventRequest;
import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import com.orque.crm.google.meet.MeetService;
import com.orque.crm.google.token.GoogleTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Every read here calls Google directly, live, on every invocation — there is no local mirror
 * table this reads from and no "Sync Now" step. Whatever Google returns right now is what the
 * CRM shows; a scheduled cache-warming job may exist purely for performance, never as the
 * source of truth for display.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarLiveService {

    private static final String CALENDAR_ID = "primary";

    private final GoogleTokenManager tokenManager;
    private final MeetService meetService;

    public List<CalendarEventDto> listEvents(String username, LocalDateTime from, LocalDateTime to) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Calendar client = buildClient(credential);
            ZoneId zone = ZoneId.systemDefault();
            Events events = client.events().list(CALENDAR_ID)
                    .setTimeMin(new DateTime(from.atZone(zone).toInstant().toEpochMilli()))
                    .setTimeMax(new DateTime(to.atZone(zone).toInstant().toEpochMilli()))
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();

            tokenManager.recordApiSuccess(username);
            return events.getItems().stream().map(this::toDto).toList();
        } catch (Exception e) {
            log.warn("Calendar list failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to fetch calendar events", e);
        }
    }

    public CalendarEventDto createEvent(String username, CreateEventRequest request) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Calendar client = buildClient(credential);
            com.google.api.services.calendar.model.Event event = toGoogleEvent(request);
            if (request.isCreateMeetLink()) {
                meetService.requestMeetLink(event);
            }

            Calendar.Events.Insert insert = client.events().insert(CALENDAR_ID, event);
            if (request.isCreateMeetLink()) {
                insert.setConferenceDataVersion(1);
            }
            com.google.api.services.calendar.model.Event created = insert.execute();

            tokenManager.recordApiSuccess(username);
            return toDto(created);
        } catch (Exception e) {
            log.warn("Calendar create failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to create calendar event", e);
        }
    }

    public CalendarEventDto updateEvent(String username, String eventId, CreateEventRequest request) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            Calendar client = buildClient(credential);
            com.google.api.services.calendar.model.Event event = toGoogleEvent(request);
            com.google.api.services.calendar.model.Event updated =
                    client.events().update(CALENDAR_ID, eventId, event).execute();
            tokenManager.recordApiSuccess(username);
            return toDto(updated);
        } catch (Exception e) {
            log.warn("Calendar update failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to update calendar event", e);
        }
    }

    public void deleteEvent(String username, String eventId) {
        GoogleWorkspaceCredential credential = tokenManager.requireConnected(username);
        try {
            buildClient(credential).events().delete(CALENDAR_ID, eventId).execute();
            tokenManager.recordApiSuccess(username);
        } catch (Exception e) {
            log.warn("Calendar delete failed for user {}: {}", username, e.getMessage());
            tokenManager.recordIfRevoked(username, e);
            throw new IllegalStateException("Failed to delete calendar event", e);
        }
    }

    private Calendar buildClient(GoogleWorkspaceCredential credential) throws Exception {
        return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
                tokenManager.buildCredential(credential))
                .setApplicationName("Orque CRM")
                .build();
    }

    private com.google.api.services.calendar.model.Event toGoogleEvent(CreateEventRequest request) {
        String zone = request.getTimeZone() != null ? request.getTimeZone() : ZoneId.systemDefault().getId();

        com.google.api.services.calendar.model.Event event = new com.google.api.services.calendar.model.Event()
                .setSummary(request.getTitle())
                .setDescription(request.getDescription())
                .setLocation(request.getLocation());

        event.setStart(new EventDateTime()
                .setDateTime(toGoogleDateTime(request.getStartDateTime(), zone))
                .setTimeZone(zone));
        event.setEnd(new EventDateTime()
                .setDateTime(toGoogleDateTime(request.getEndDateTime(), zone))
                .setTimeZone(zone));

        if (request.getAttendees() != null && !request.getAttendees().isEmpty()) {
            event.setAttendees(request.getAttendees().stream()
                    .map(email -> new EventAttendee().setEmail(email))
                    .toList());
        }

        if (request.getRecurrenceRule() != null && !request.getRecurrenceRule().isBlank()) {
            event.setRecurrence(List.of(request.getRecurrenceRule()));
        }

        return event;
    }

    private DateTime toGoogleDateTime(String isoLocal, String zoneId) {
        ZonedDateTime zdt = LocalDateTime.parse(isoLocal).atZone(ZoneId.of(zoneId));
        return new DateTime(zdt.toInstant().toEpochMilli());
    }

    private CalendarEventDto toDto(com.google.api.services.calendar.model.Event event) {
        return CalendarEventDto.builder()
                .id(event.getId())
                .title(event.getSummary())
                .description(event.getDescription())
                .location(event.getLocation())
                .startDateTime(toIso(event.getStart()))
                .endDateTime(toIso(event.getEnd()))
                .timeZone(event.getStart() != null ? event.getStart().getTimeZone() : null)
                .allDay(event.getStart() != null && event.getStart().getDate() != null)
                .recurrenceRule(event.getRecurrence() != null && !event.getRecurrence().isEmpty()
                        ? event.getRecurrence().get(0) : null)
                .attendees(event.getAttendees() != null
                        ? event.getAttendees().stream().map(EventAttendee::getEmail).toList() : null)
                .meetLink(meetService.extractMeetLink(event))
                .conferenceId(meetService.extractConferenceId(event))
                .build();
    }

    private String toIso(EventDateTime dt) {
        if (dt == null) return null;
        if (dt.getDateTime() != null) {
            return java.time.Instant.ofEpochMilli(dt.getDateTime().getValue())
                    .atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
        }
        return Optional.ofNullable(dt.getDate()).map(DateTime::toString).orElse(null);
    }
}
