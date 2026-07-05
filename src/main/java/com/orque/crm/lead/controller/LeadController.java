package com.orque.crm.lead.controller;

import com.orque.crm.auth.entity.User;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.lead.dto.*;
import com.orque.crm.lead.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            request.setAssignedOwner(u.getUsername());
        }
        return leadService.createLead(request);
    }

    @PostMapping("/convert/{contactId}")
    public LeadResponse convertContactToLead(
            @PathVariable Long contactId,
            @RequestBody ConvertContactToLeadRequest request
    ) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            request.setAssignedOwner(u.getUsername());
        }
        return leadService.convertContactToLead(contactId, request);
    }

    @PostMapping("/convert/bulk")
    public List<LeadResponse> bulkConvertContactsToLeads(
            @Valid @RequestBody BulkConvertContactsRequest request
    ) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            request.setAssignedOwner(u.getUsername());
        }
        return leadService.bulkConvertContactsToLeads(request);
    }

    @GetMapping
    public List<LeadResponse> getAllLeads() {
        List<LeadResponse> leads = leadService.getAllLeads();
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            String roleName = u.getRole() != null ? u.getRole().getName().name() : "";
            if (!"ADMIN".equals(roleName) && !"SALES_ADMIN".equals(roleName)) {
                return leads.stream()
                        .filter(l -> l.getAssignedOwner() == null || l.getAssignedOwner().trim().isEmpty() || l.getAssignedOwner().equalsIgnoreCase(u.getUsername()))
                        .toList();
            }
        }
        return leads;
    }

    @GetMapping("/{id}")
    public LeadResponse getLeadById(
            @PathVariable Long id
    ) {
        LeadResponse lead = leadService.getLeadById(id);
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            String roleName = u.getRole() != null ? u.getRole().getName().name() : "";
            if (!"ADMIN".equals(roleName) && !"SALES_ADMIN".equals(roleName)) {
                if (lead.getAssignedOwner() != null && !lead.getAssignedOwner().equalsIgnoreCase(u.getUsername())) {
                    throw new RuntimeException("Access denied.");
                }
            }
        }
        return lead;
    }

    @GetMapping("/{id}/activities")
    public List<LeadActivityResponse> getLeadActivities(
            @PathVariable Long id
    ) {
        return leadService.getLeadActivities(id);
    }

    @PutMapping("/{id}")
    public LeadResponse updateLead(
            @PathVariable Long id,
            @RequestBody CreateLeadRequest request
    ) {
        LeadResponse existing = leadService.getLeadById(id);
        UserContextHelper.assertAccess(existing.getAssignedOwner());
        // Preserve original owner — edit does not reassign
        request.setAssignedOwner(existing.getAssignedOwner());
        return leadService.updateLead(id, request);
    }

    @PostMapping("/qualify/{id}")
    public LeadResponse promoteToQualified(@PathVariable Long id) {
        return leadService.promoteToQualified(id);
    }

    @PostMapping("/submit/{id}")
    public LeadResponse qualifyLead(@PathVariable Long id) {
        return leadService.qualifyLead(id);
    }

    @PostMapping("/bulk-import")
    public List<LeadResponse> bulkImportLeads(@RequestBody List<CreateLeadRequest> requests) {
        String owner = UserContextHelper.currentUsername();
        requests.forEach(r -> r.setAssignedOwner(owner));
        return leadService.bulkImportLeads(requests);
    }

    @DeleteMapping("/{id}")
    public void deleteLead(@PathVariable Long id) {
        LeadResponse existing = leadService.getLeadById(id);
        UserContextHelper.assertAccess(existing.getAssignedOwner());
        leadService.deleteLead(id);
    }
}