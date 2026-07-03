package com.orque.crm.settings.smtp.controller;

import com.orque.crm.settings.smtp.dto.SmtpConfigRequest;
import com.orque.crm.settings.smtp.dto.SmtpConfigResponse;
import com.orque.crm.settings.smtp.dto.SmtpTestResult;
import com.orque.crm.settings.smtp.service.SmtpConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/smtp-configs")
@RequiredArgsConstructor
@CrossOrigin
public class SmtpConfigController {

    private final SmtpConfigService service;

    @GetMapping
    public ResponseEntity<List<SmtpConfigResponse>> list() {
        return ResponseEntity.ok(service.listForCurrentUser());
    }

    @PostMapping
    public ResponseEntity<SmtpConfigResponse> create(@RequestBody SmtpConfigRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SmtpConfigResponse> update(@PathVariable Long id,
                                                     @RequestBody SmtpConfigRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<SmtpTestResult> testConnection(@PathVariable Long id) {
        return ResponseEntity.ok(service.testConnection(id));
    }
}
