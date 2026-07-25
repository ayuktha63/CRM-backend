package com.orque.crm.google.meet;

import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.Event;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Google Meet has no API of its own — a Meet link is just conferenceData on a Calendar event.
 * This service exists as its own module (per the architecture split) purely to keep that detail
 * out of {@code GoogleCalendarLiveService}, so Calendar logic doesn't need to know Meet's
 * request/response shape.
 */
@Service
public class MeetService {

    /** Attaches a "please create a Meet link" request to an event about to be inserted. */
    public void requestMeetLink(Event event) {
        event.setConferenceData(new ConferenceData()
                .setCreateRequest(new CreateConferenceRequest()
                        .setRequestId(UUID.randomUUID().toString())
                        .setConferenceSolutionKey(new ConferenceSolutionKey().setType("hangoutsMeet"))));
    }

    /** Google returns the actual link/ID async — only present once the create request resolved. */
    public String extractMeetLink(Event event) {
        return event.getHangoutLink();
    }

    public String extractConferenceId(Event event) {
        if (event.getConferenceData() == null || event.getConferenceData().getConferenceId() == null) {
            return null;
        }
        return event.getConferenceData().getConferenceId();
    }
}
