package com.orque.crm.organization.controller;

import com.orque.crm.organization.dto.OrganizationRequest;
import com.orque.crm.organization.dto.OrganizationResponse;
import com.orque.crm.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> listAll() {
        return ResponseEntity.ok(organizationService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable String id,
            @Valid @RequestBody OrganizationRequest request) {
        return ResponseEntity.ok(organizationService.update(id, request));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<String> suspend(@PathVariable String id) {
        organizationService.suspend(id);
        return ResponseEntity.ok("Organization suspended.");
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<String> activate(@PathVariable String id) {
        organizationService.activate(id);
        return ResponseEntity.ok("Organization activated.");
    }
}
