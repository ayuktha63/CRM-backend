package com.orque.crm.feature.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@CrossOrigin
public class SystemSettingsController {

    @GetMapping("/license")
    public ResponseEntity<Map<String, String>> getLicenseInfo() {
        return ResponseEntity.ok(Map.of(
                "startDate", "2026-01-01",
                "endDate", "2026-12-31",
                "gracePeriod", "30 days"
        ));
    }
}
