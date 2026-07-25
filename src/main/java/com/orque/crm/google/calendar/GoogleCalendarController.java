package com.orque.crm.google.calendar;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.google.calendar.dto.CalendarEventDto;
import com.orque.crm.google.calendar.dto.CreateEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/google/calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarLiveService calendarService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> listEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(calendarService.listEvents(UserContextHelper.currentUsername(), from, to));
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventDto> createEvent(@RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(calendarService.createEvent(UserContextHelper.currentUsername(), request));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<CalendarEventDto> updateEvent(@PathVariable String id, @RequestBody CreateEventRequest request) {
        return ResponseEntity.ok(calendarService.updateEvent(UserContextHelper.currentUsername(), id, request));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable String id) {
        calendarService.deleteEvent(UserContextHelper.currentUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
