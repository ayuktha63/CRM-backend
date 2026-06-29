package com.orque.crm.task.controller;

import com.orque.crm.task.entity.CrmCalendarEvent;
import com.orque.crm.task.service.CrmCalendarEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar-events")
@RequiredArgsConstructor
@CrossOrigin
public class CrmCalendarEventController {

    private final CrmCalendarEventService service;

    @GetMapping
    public ResponseEntity<List<CrmCalendarEvent>> getEvents() {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(service.getEvents(username));
    }

    @PostMapping
    public ResponseEntity<CrmCalendarEvent> saveEvent(@RequestBody CrmCalendarEvent event) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(service.saveEvent(event, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        service.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/export-ics")
    public ResponseEntity<String> exportIcs(@PathVariable Long id) {
        String ics = service.exportEventToIcs(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"event_" + id + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(ics);
    }

    @PostMapping("/import-ics")
    public ResponseEntity<CrmCalendarEvent> importIcs(@RequestBody String icsContent) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(service.importEventFromIcs(icsContent, username));
    }
}
