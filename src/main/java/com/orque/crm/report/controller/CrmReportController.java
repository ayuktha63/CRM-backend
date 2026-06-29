package com.orque.crm.report.controller;

import com.orque.crm.report.entity.CrmReport;
import com.orque.crm.report.service.CrmReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/crm-reports")
@RequiredArgsConstructor
@CrossOrigin
public class CrmReportController {

    private final CrmReportService service;

    @GetMapping
    public ResponseEntity<List<CrmReport>> getReports() {
        return ResponseEntity.ok(service.getReports());
    }

    @PostMapping
    public ResponseEntity<CrmReport> saveReport(@RequestBody CrmReport report) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(service.saveReport(report, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        service.deleteReport(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/run")
    public ResponseEntity<List<Map<String, Object>>> executeReport(@PathVariable Long id) {
        return ResponseEntity.ok(service.executeReport(id));
    }
}
