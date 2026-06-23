package com.orque.crm.lead.controller;

import com.orque.crm.lead.dto.*;
import com.orque.crm.lead.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public LeadResponse createLead(
            @Valid @RequestBody CreateLeadRequest request
    ) {
        return leadService.createLead(request);
    }

    @PostMapping("/convert/{contactId}")
    public LeadResponse convertContactToLead(
            @PathVariable Long contactId,
            @RequestBody ConvertContactToLeadRequest request
    ) {
        return leadService.convertContactToLead(contactId, request);
    }

    @PostMapping("/convert/bulk")
    public List<LeadResponse> bulkConvertContactsToLeads(
            @Valid @RequestBody BulkConvertContactsRequest request
    ) {
        return leadService.bulkConvertContactsToLeads(request);
    }

    @GetMapping
    public List<LeadResponse> getAllLeads() {
        return leadService.getAllLeads();
    }

    @GetMapping("/{id}")
    public LeadResponse getLeadById(
            @PathVariable Long id
    ) {
        return leadService.getLeadById(id);
    }

    @GetMapping("/{id}/activities")
    public List<LeadActivityResponse> getLeadActivities(
            @PathVariable Long id
    ) {
        return leadService.getLeadActivities(id);
    }
}