package com.orque.crm.settings.metadata.controller;

import com.orque.crm.settings.metadata.entity.CrmMetadataField;
import com.orque.crm.settings.metadata.entity.CrmMetadataModule;
import com.orque.crm.settings.metadata.service.MetadataService;
import com.orque.crm.common.UserContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
@CrossOrigin
public class MetadataController {

    private final MetadataService service;

    @GetMapping("/modules")
    public ResponseEntity<List<CrmMetadataModule>> getModules() {
        return ResponseEntity.ok(service.getModules());
    }

    @PostMapping("/modules")
    public ResponseEntity<CrmMetadataModule> createModule(
            @RequestParam String name,
            @RequestParam String label) {
        return ResponseEntity.ok(service.createModule(name, label));
    }

    @GetMapping("/fields/{moduleName}")
    public ResponseEntity<List<CrmMetadataField>> getFields(@PathVariable String moduleName) {
        return ResponseEntity.ok(service.getFields(moduleName));
    }

    @PostMapping("/fields/{moduleName}")
    public ResponseEntity<CrmMetadataField> createField(
            @PathVariable String moduleName,
            @RequestParam String name,
            @RequestParam String label,
            @RequestParam String type,
            @RequestParam(required = false) Boolean required,
            @RequestParam(required = false) Boolean readonly,
            @RequestParam(required = false) String options,
            @RequestParam(required = false) String formula,
            @RequestParam(required = false) String lookupModule) {
        return ResponseEntity.ok(service.createField(
                moduleName, name, label, type, required, readonly, options, formula, lookupModule));
    }

    @GetMapping("/custom-records/{moduleName}")
    public ResponseEntity<List<Map<String, Object>>> getCustomRecords(@PathVariable String moduleName) {
        return ResponseEntity.ok(service.getCustomRecords(moduleName));
    }

    @GetMapping("/custom-records/{moduleName}/{id}")
    public ResponseEntity<Map<String, Object>> getCustomRecord(
            @PathVariable String moduleName,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getCustomRecord(moduleName, id));
    }

    @PostMapping("/custom-records/{moduleName}")
    public ResponseEntity<Map<String, Object>> saveCustomRecord(
            @PathVariable String moduleName,
            @RequestBody Map<String, Object> values) {
        String username = "system";
        try {
            username = com.orque.crm.common.UserContextHelper.currentUsername();
        } catch (Exception e) {
            // fallback if context helper is in different package
        }
        Long recordId = null;
        if (values.containsKey("id") && values.get("id") != null) {
            recordId = Long.valueOf(String.valueOf(values.get("id")));
        }
        return ResponseEntity.ok(service.saveCustomRecord(moduleName, recordId, values, username));
    }
}
