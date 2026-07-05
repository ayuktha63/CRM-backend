package com.orque.crm.tax.controller;

import com.orque.crm.tax.dto.TaxSettingsRequest;
import com.orque.crm.tax.dto.TaxSettingsResponse;
import com.orque.crm.tax.service.OrganizationTaxSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tax-settings")
@RequiredArgsConstructor
@CrossOrigin
public class TaxSettingsController {

    private final OrganizationTaxSettingsService service;

    @GetMapping("/me")
    public ResponseEntity<TaxSettingsResponse> getMySettings() {
        return ResponseEntity.ok(service.getMySettings());
    }

    @PutMapping("/me")
    public ResponseEntity<TaxSettingsResponse> updateMySettings(@RequestBody TaxSettingsRequest request) {
        return ResponseEntity.ok(service.updateMySettings(request));
    }
}
