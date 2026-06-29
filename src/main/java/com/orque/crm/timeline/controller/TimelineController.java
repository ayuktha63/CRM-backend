package com.orque.crm.timeline.controller;

import com.orque.crm.timeline.entity.TimelineEntry;
import com.orque.crm.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/timeline")
@RequiredArgsConstructor
@CrossOrigin
public class TimelineController {

    private final TimelineService service;

    @GetMapping("/{moduleName}/{recordId}")
    public ResponseEntity<List<TimelineEntry>> getTimeline(
            @PathVariable String moduleName,
            @PathVariable Long recordId) {
        return ResponseEntity.ok(service.getTimeline(moduleName, recordId));
    }
}
