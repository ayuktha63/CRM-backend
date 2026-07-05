package com.orque.crm.task.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.EventDateTime;
import com.orque.crm.task.config.GoogleCalendarOAuthProperties;
import com.orque.crm.task.entity.ConnectedGoogleCalendar;
import com.orque.crm.task.entity.CrmCalendarEvent;
import com.orque.crm.task.repository.ConnectedGoogleCalendarRepository;
import com.orque.crm.task.repository.CrmCalendarEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarSyncServiceImpl implements GoogleCalendarSyncService {

    private static final String CALENDAR_ID = "primary";
    private static final String SYNC_SOURCE = "GOOGLE_CALENDAR";

    private final ConnectedGoogleCalendarRepository connectedGoogleCalendarRepository;
    private final CrmCalendarEventRepository crmCalendarEventRepository;
    private final GoogleCalendarOAuthProperties properties;

    @Override
    public void pushEvent(CrmCalendarEvent event) {
        if (event.getCreatedBy() == null) return;
        ConnectedGoogleCalendar connection = connectedGoogleCalendarRepository
                .findByOwnerIgnoreCase(event.getCreatedBy())
                .filter(c -> Boolean.TRUE.equals(c.getSyncEnabled()))
                .orElse(null);
        if (connection == null) return;

        try {
            Calendar client = buildClient(connection);
            com.google.api.services.calendar.model.Event googleEvent = toGoogleEvent(event);

            if (event.getSyncId() != null && !event.getSyncId().isBlank()) {
                try {
                    client.events().update(CALENDAR_ID, event.getSyncId(), googleEvent).execute();
                    return;
                } catch (Exception notFound) {
                    log.info("Google event {} no longer exists remotely, recreating", event.getSyncId());
                }
            }

            com.google.api.services.calendar.model.Event created =
                    client.events().insert(CALENDAR_ID, googleEvent).execute();
            event.setSyncId(created.getId());
            event.setSyncSource(SYNC_SOURCE);
            crmCalendarEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to push CRM event {} to Google Calendar for user {}: {}",
                    event.getId(), event.getCreatedBy(), e.getMessage());
        }
    }

    @Override
    public void deleteRemoteEvent(CrmCalendarEvent event) {
        if (event.getSyncId() == null || event.getCreatedBy() == null) return;
        connectedGoogleCalendarRepository.findByOwnerIgnoreCase(event.getCreatedBy())
                .ifPresent(connection -> {
                    try {
                        buildClient(connection).events().delete(CALENDAR_ID, event.getSyncId()).execute();
                    } catch (Exception e) {
                        log.info("Could not delete remote Google event {}: {}", event.getSyncId(), e.getMessage());
                    }
                });
    }

    /** Auto two-way sync for every connected account, every 5 minutes. */
    @Scheduled(fixedRate = 300_000L)
    public void autoSyncAll() {
        for (ConnectedGoogleCalendar connection : connectedGoogleCalendarRepository.findBySyncEnabledTrue()) {
            syncNow(connection.getOwner());
        }
    }

    @Override
    public Map<String, Object> syncNow(String username) {
        Map<String, Object> result = new HashMap<>();
        ConnectedGoogleCalendar connection = connectedGoogleCalendarRepository.findByOwnerIgnoreCase(username).orElse(null);
        if (connection == null) {
            result.put("success", false);
            result.put("reason", "not_connected");
            return result;
        }

        int pulled = 0;
        int pushed = 0;
        try {
            Calendar client = buildClient(connection);

            // Pull: bring remote events into the CRM (create or update by syncId).
            DateTime since = new DateTime(
                    (connection.getLastSyncedAt() != null ? connection.getLastSyncedAt() : LocalDateTime.now().minusDays(30))
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());

            List<com.google.api.services.calendar.model.Event> items = client.events().list(CALENDAR_ID)
                    .setUpdatedMin(since)
                    .setSingleEvents(true)
                    .setShowDeleted(true)
                    .execute()
                    .getItems();

            for (com.google.api.services.calendar.model.Event googleEvent : items) {
                if (googleEvent.getStatus() != null && googleEvent.getStatus().equals("cancelled")) {
                    crmCalendarEventRepository.findBySyncId(googleEvent.getId())
                            .ifPresent(crmCalendarEventRepository::delete);
                    continue;
                }
                if (googleEvent.getStart() == null || googleEvent.getStart().getDateTime() == null) continue; // skip all-day events

                Optional<CrmCalendarEvent> existing = crmCalendarEventRepository.findBySyncId(googleEvent.getId());
                CrmCalendarEvent local = existing.orElseGet(() -> CrmCalendarEvent.builder()
                        .createdBy(username)
                        .organizationId(connection.getOrganizationId())
                        .syncId(googleEvent.getId())
                        .syncSource(SYNC_SOURCE)
                        .build());

                local.setTitle(googleEvent.getSummary() != null ? googleEvent.getSummary() : "(no title)");
                local.setDescription(googleEvent.getDescription());
                local.setStartDateTime(toLocalDateTime(googleEvent.getStart()));
                local.setEndDateTime(toLocalDateTime(googleEvent.getEnd()));
                crmCalendarEventRepository.save(local);
                pulled++;
            }

            // Push: send up any locally-created events never yet synced.
            List<CrmCalendarEvent> unsynced = crmCalendarEventRepository
                    .findByOrganizationIdAndCreatedByIgnoreCase(connection.getOrganizationId(), username)
                    .stream()
                    .filter(e -> e.getSyncId() == null)
                    .toList();
            for (CrmCalendarEvent event : unsynced) {
                pushEvent(event);
                pushed++;
            }

            connection.setLastSyncedAt(LocalDateTime.now());
            connectedGoogleCalendarRepository.save(connection);

            result.put("success", true);
            result.put("provider", "Google Calendar");
            result.put("googleEmail", connection.getGoogleEmail());
            result.put("pulledCount", pulled);
            result.put("pushedCount", pushed);
            result.put("lastSynced", connection.getLastSyncedAt().toString());
        } catch (Exception e) {
            log.warn("Google Calendar sync failed for user {}: {}", username, e.getMessage());
            result.put("success", false);
            result.put("reason", "sync_failed");
        }
        return result;
    }

    private Calendar buildClient(ConnectedGoogleCalendar connection) throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .setClientSecrets(properties.getClientId(), properties.getClientSecret())
                .build();
        credential.setAccessToken(connection.getAccessToken());
        credential.setRefreshToken(connection.getRefreshToken());

        return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("Orque CRM")
                .build();
    }

    private com.google.api.services.calendar.model.Event toGoogleEvent(CrmCalendarEvent event) {
        String zone = event.getTimeZone() != null ? event.getTimeZone() : ZoneId.systemDefault().getId();

        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event()
                .setSummary(event.getTitle())
                .setDescription(event.getDescription())
                .setLocation(event.getMeetingRoom());

        googleEvent.setStart(new EventDateTime()
                .setDateTime(new DateTime(event.getStartDateTime().atZone(ZoneId.of(zone)).toInstant().toEpochMilli()))
                .setTimeZone(zone));
        googleEvent.setEnd(new EventDateTime()
                .setDateTime(new DateTime(event.getEndDateTime().atZone(ZoneId.of(zone)).toInstant().toEpochMilli()))
                .setTimeZone(zone));

        return googleEvent;
    }

    private LocalDateTime toLocalDateTime(EventDateTime eventDateTime) {
        long millis = eventDateTime.getDateTime().getValue();
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
}
