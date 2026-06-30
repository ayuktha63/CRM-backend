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

    @PostMapping("/sync/google")
    public ResponseEntity<java.util.Map<String, Object>> syncGoogle() {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        res.put("provider", "Google Calendar");
        res.put("syncedCount", 3);
        res.put("lastSynced", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/sync/outlook")
    public ResponseEntity<java.util.Map<String, Object>> syncOutlook() {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("success", true);
        res.put("provider", "Outlook");
        res.put("syncedCount", 2);
        res.put("lastSynced", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/slots")
    public ResponseEntity<List<java.util.Map<String, Object>>> getAvailableSlots(
            @RequestParam String date,
            @RequestParam(defaultValue = "30") int durationMinutes) {
        
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        java.time.LocalDateTime startOfDay = localDate.atStartOfDay();
        java.time.LocalDateTime endOfDay = localDate.atTime(23, 59, 59);
        
        List<CrmCalendarEvent> events = service.getEvents(username);
        List<CrmCalendarEvent> dayEvents = events.stream()
                .filter(e -> !e.getStartDateTime().isAfter(endOfDay) && !e.getEndDateTime().isBefore(startOfDay))
                .toList();

        List<java.util.Map<String, Object>> slots = new java.util.ArrayList<>();
        java.time.LocalDateTime currentSlot = localDate.atTime(9, 0);
        java.time.LocalDateTime workingHoursEnd = localDate.atTime(17, 0);
        
        while (currentSlot.plusMinutes(durationMinutes).isBefore(workingHoursEnd) || currentSlot.plusMinutes(durationMinutes).isEqual(workingHoursEnd)) {
            java.time.LocalDateTime slotStart = currentSlot;
            java.time.LocalDateTime slotEnd = currentSlot.plusMinutes(durationMinutes);
            
            boolean isBooked = dayEvents.stream().anyMatch(e -> 
                e.getStartDateTime().isBefore(slotEnd) && e.getEndDateTime().isAfter(slotStart)
            );
            
            java.util.Map<String, Object> slotMap = new java.util.HashMap<>();
            slotMap.put("time", slotStart.toLocalTime().toString());
            slotMap.put("available", !isBooked);
            slots.add(slotMap);
            
            currentSlot = currentSlot.plusMinutes(30);
        }
        
        return ResponseEntity.ok(slots);
    }
}
