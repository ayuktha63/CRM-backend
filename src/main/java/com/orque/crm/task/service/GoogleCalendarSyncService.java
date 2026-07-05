package com.orque.crm.task.service;

import com.orque.crm.task.entity.CrmCalendarEvent;

public interface GoogleCalendarSyncService {

    /** Creates or updates the corresponding Google Calendar event for a locally-saved CRM event. */
    void pushEvent(CrmCalendarEvent event);

    /** Deletes the corresponding Google Calendar event, if one exists, when a CRM event is deleted. */
    void deleteRemoteEvent(CrmCalendarEvent event);

    /** Pulls new/updated events from the user's Google Calendar into the CRM, and pushes local ones out. */
    java.util.Map<String, Object> syncNow(String username);
}
