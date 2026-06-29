package com.orque.crm.report.controller;

import com.orque.crm.report.entity.CrmDashboard;
import com.orque.crm.report.service.CrmDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crm-dashboards")
@RequiredArgsConstructor
@CrossOrigin
public class CrmDashboardController {

    private final CrmDashboardService service;

    @GetMapping
    public ResponseEntity<List<CrmDashboard>> getDashboards() {
        return ResponseEntity.ok(service.getDashboards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CrmDashboard> getDashboard(@PathVariable Long id) {
        return ResponseEntity.ok(service.getDashboard(id));
    }

    @PostMapping
    public ResponseEntity<CrmDashboard> saveDashboard(@RequestBody CrmDashboard dashboard) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback
        }
        return ResponseEntity.ok(service.saveDashboard(dashboard, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDashboard(@PathVariable Long id) {
        service.deleteDashboard(id);
        return ResponseEntity.noContent().build();
    }
}
