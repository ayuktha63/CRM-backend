package com.orque.crm.task.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.google.calendar.GoogleCalendarLiveService;
import com.orque.crm.google.calendar.dto.CalendarEventDto;
import com.orque.crm.google.calendar.dto.CreateEventRequest;
import com.orque.crm.task.entity.CrmCalendarEvent;
import com.orque.crm.task.repository.CrmCalendarEventRepository;
import com.orque.crm.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrmCalendarEventService {

    private final CrmCalendarEventRepository repository;
    private final NotificationService notificationService;
    private final GoogleCalendarLiveService googleCalendarLiveService;

    private static final DateTimeFormatter ICS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public List<CrmCalendarEvent> getEvents(String username) {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner(); // null for org admins -> see everyone in the org
        if (owner == null) {
            return orgId != null ? repository.findByOrganizationId(orgId) : repository.findByCreatedByIgnoreCase(username);
        }
        return orgId != null
                ? repository.findByOrganizationIdAndCreatedByIgnoreCase(orgId, owner)
                : repository.findByCreatedByIgnoreCase(owner);
    }

    @Transactional
    public CrmCalendarEvent saveEvent(CrmCalendarEvent event, String username) {
        event.setCreatedBy(username);
        event.setOrganizationId(UserContextHelper.currentOrganizationId());
        CrmCalendarEvent saved = repository.save(event);

        // Schedule notification reminder if configured
        if (saved.getReminderMinutes() != null && saved.getReminderMinutes() > 0) {
            notificationService.addNotification(
                    username,
                    "Meeting Reminder: " + saved.getTitle(),
                    "Meeting starts on " + saved.getStartDateTime() + " in room " + saved.getMeetingRoom(),
                    "/calendar"
            );
        }

        return saved;
    }

    /** Pushes every local event that's never been synced (syncSource == null) into the user's
     *  real Google Calendar. Marks each row with syncSource/syncId right after its own push
     *  succeeds, so a partial failure doesn't duplicate the ones that already went through. */
    @Transactional
    public int syncToGoogle(String username) {
        List<CrmCalendarEvent> unsynced = repository.findByCreatedByIgnoreCaseAndSyncSourceIsNull(username);
        int count = 0;
        for (CrmCalendarEvent event : unsynced) {
            try {
                CreateEventRequest request = new CreateEventRequest();
                request.setTitle(event.getTitle());
                request.setDescription(event.getDescription());
                request.setLocation(event.getMeetingRoom());
                request.setStartDateTime(event.getStartDateTime().toString());
                request.setEndDateTime(event.getEndDateTime().toString());
                request.setTimeZone(event.getTimeZone());
                if (event.getInvitees() != null && !event.getInvitees().isBlank()) {
                    request.setAttendees(Arrays.stream(event.getInvitees().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).toList());
                }
                if (event.getRecurrenceRule() != null && !"NONE".equals(event.getRecurrenceRule())) {
                    request.setRecurrenceRule("RRULE:FREQ=" + event.getRecurrenceRule());
                }

                CalendarEventDto created = googleCalendarLiveService.createEvent(username, request);
                event.setSyncSource("google");
                event.setSyncId(created.getId());
                repository.save(event);
                count++;
            } catch (Exception e) {
                log.warn("Failed to sync calendar event {} to Google for user {}: {}", event.getId(), username, e.getMessage());
            }
        }
        return count;
    }

    public long countUnsynced(String username) {
        return repository.findByCreatedByIgnoreCaseAndSyncSourceIsNull(username).size();
    }

    @Transactional
    public void deleteEvent(Long id) {
        repository.findById(id).ifPresent(event -> {
            UserContextHelper.assertAccess(event.getOrganizationId(), event.getCreatedBy());
            repository.deleteById(id);
        });
    }

    public String exportEventToIcs(Long eventId) {
        CrmCalendarEvent event = repository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found"));
        UserContextHelper.assertAccess(event.getOrganizationId(), event.getCreatedBy());

        StringBuilder ics = new StringBuilder();
        ics.append("BEGIN:VCALENDAR\r\n");
        ics.append("VERSION:2.0\r\n");
        ics.append("PRODID:-//Orque CRM//Calendar Workspace//EN\r\n");
        ics.append("BEGIN:VEVENT\r\n");
        ics.append("UID:").append(event.getId()).append("@orque.crm\r\n");
        ics.append("SUMMARY:").append(event.getTitle()).append("\r\n");
        ics.append("DESCRIPTION:").append(event.getDescription() != null ? event.getDescription() : "").append("\r\n");
        ics.append("DTSTART:").append(event.getStartDateTime().format(ICS_DATE_FORMAT)).append("\r\n");
        ics.append("DTEND:").append(event.getEndDateTime().format(ICS_DATE_FORMAT)).append("\r\n");
        ics.append("LOCATION:").append(event.getMeetingRoom() != null ? event.getMeetingRoom() : "").append("\r\n");
        ics.append("END:VEVENT\r\n");
        ics.append("END:VCALENDAR\r\n");

        return ics.toString();
    }

    @Transactional
    public CrmCalendarEvent importEventFromIcs(String icsContent, String username) {
        String title = "Imported ICS Event";
        String description = "";
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        String room = "";

        String[] lines = icsContent.split("\\r?\\n");
        for (String line : lines) {
            if (line.startsWith("SUMMARY:")) {
                title = line.substring(8);
            } else if (line.startsWith("DESCRIPTION:")) {
                description = line.substring(12);
            } else if (line.startsWith("LOCATION:")) {
                room = line.substring(9);
            } else if (line.startsWith("DTSTART:")) {
                try {
                    String dateStr = line.substring(8).trim();
                    start = LocalDateTime.parse(dateStr, ICS_DATE_FORMAT);
                } catch (Exception e) {
                    // Fallback to default
                }
            } else if (line.startsWith("DTEND:")) {
                try {
                    String dateStr = line.substring(6).trim();
                    end = LocalDateTime.parse(dateStr, ICS_DATE_FORMAT);
                } catch (Exception e) {
                    // Fallback to default
                }
            }
        }

        CrmCalendarEvent event = CrmCalendarEvent.builder()
                .title(title)
                .description(description)
                .startDateTime(start)
                .endDateTime(end)
                .meetingRoom(room)
                .createdBy(username)
                .organizationId(UserContextHelper.currentOrganizationId())
                .build();

        return repository.save(event);
    }
}
