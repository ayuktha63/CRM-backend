package com.orque.crm.lead.service;

import com.orque.crm.audit.service.AuditLogService;
import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.enums.*;
import com.orque.crm.lead.dto.*;
import com.orque.crm.lead.entity.Lead;
import com.orque.crm.lead.entity.LeadActivity;
import com.orque.crm.lead.repository.LeadActivityRepository;
import com.orque.crm.lead.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.orque.crm.enums.AuditModule;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final ContactRepository contactRepository;
    private final AuditLogService auditLogService;

    @Override
    public LeadResponse createLead(CreateLeadRequest request) {

        if (leadRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Lead with this email already exists");
        }

        Lead lead = Lead.builder()
                .contactId(null)
                .fullName(request.getFullName())
                .company(request.getCompany())
                .email(request.getEmail())
                .phone(request.getPhone())
                .jobTitle(request.getJobTitle())
                .industry(request.getIndustry())
                .website(request.getWebsite())
                .address(request.getAddress())
                .country(request.getCountry())
                .state(request.getState())
                .city(request.getCity())
                .tags(request.getTags())
                .leadSource(
                        request.getLeadSource() != null
                                ? request.getLeadSource()
                                : LeadSource.MANUAL
                )
                .assignedOwner(request.getAssignedOwner())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : LeadStatus.NEW
                )
                .pipelineStage(PipelineStage.NEW)
                .notes(request.getNotes())
                .estimatedValue(request.getEstimatedValue())
                .expectedCloseDate(request.getExpectedCloseDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Lead savedLead = leadRepository.save(lead);

        createLeadActivity(
                savedLead.getId(),
                LeadActivityType.LEAD_CREATED,
                "Lead created manually"
        );

        auditLogService.createAudit(
                AuditAction.LEAD_CREATED,
                AuditModule.LEAD,
                "Lead",
                savedLead.getId(),
                null,
                savedLead.getFullName(),
                "Lead created manually: " + savedLead.getFullName(),
                savedLead.getAssignedOwner(),
                null
        );

        return mapToLeadResponse(savedLead);
    }

    @Override
    public LeadResponse convertContactToLead(
            Long contactId,
            ConvertContactToLeadRequest request
    ) {

        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        if (leadRepository.existsByContactId(contactId)) {
            throw new RuntimeException("Contact is already converted to lead");
        }

        if (leadRepository.existsByEmail(contact.getEmail())) {
            throw new RuntimeException("Lead with this contact email already exists");
        }

        Lead lead = Lead.builder()
                .contactId(contact.getId())
                .fullName(contact.getFullName())
                .company(contact.getCompany())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .jobTitle(contact.getJobTitle())
                .industry(contact.getIndustry())
                .website(contact.getWebsite())
                .address(contact.getAddress())
                .country(contact.getCountry())
                .state(contact.getState())
                .city(contact.getCity())
                .tags(contact.getTags())
                .leadSource(LeadSource.CONTACT_CONVERSION)
                .assignedOwner(request.getAssignedOwner())
                .status(
                        request.getStatus() != null
                                ? request.getStatus()
                                : LeadStatus.NEW
                )
                .pipelineStage(PipelineStage.NEW)
                .notes(request.getNotes())
                .estimatedValue(request.getEstimatedValue())
                .expectedCloseDate(request.getExpectedCloseDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Lead savedLead = leadRepository.save(lead);

        contact.setStatus(ContactStatus.CONVERTED_TO_LEAD);
        contact.setUpdatedAt(LocalDateTime.now());
        contactRepository.save(contact);

        createLeadActivity(
                savedLead.getId(),
                LeadActivityType.CONTACT_CONVERTED,
                "Contact converted into lead: " + contact.getFullName()
        );

        auditLogService.createAudit(
                AuditAction.CONTACT_CONVERTED_TO_LEAD,
                AuditModule.LEAD,
                "Lead",
                savedLead.getId(),
                null,
                "Contact ID " + contactId + " converted into Lead ID " + savedLead.getId(),
                "Contact ID " + contactId + " converted into Lead ID " + savedLead.getId(),
                savedLead.getAssignedOwner(),
                null
        );

        return mapToLeadResponse(savedLead);
    }

    @Override
    public List<LeadResponse> bulkConvertContactsToLeads(
            BulkConvertContactsRequest request
    ) {
        return request.getContactIds()
                .stream()
                .map(contactId -> convertContactToLead(
                        contactId,
                        ConvertContactToLeadRequest.builder()
                                .assignedOwner(request.getAssignedOwner())
                                .status(request.getStatus())
                                .notes(request.getNotes())
                                .estimatedValue(request.getEstimatedValue())
                                .expectedCloseDate(request.getExpectedCloseDate())
                                .build()
                ))
                .toList();
    }

    @Override
    public List<LeadResponse> getAllLeads() {
        return leadRepository.findAll()
                .stream()
                .map(this::mapToLeadResponse)
                .toList();
    }

    @Override
    public LeadResponse getLeadById(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found"));

        return mapToLeadResponse(lead);
    }

    @Override
    public List<LeadActivityResponse> getLeadActivities(Long leadId) {
        return leadActivityRepository.findByLeadIdOrderByCreatedAtDesc(leadId)
                .stream()
                .map(this::mapToActivityResponse)
                .toList();
    }

    private void createLeadActivity(
            Long leadId,
            LeadActivityType activityType,
            String description
    ) {
        LeadActivity activity = LeadActivity.builder()
                .leadId(leadId)
                .activityType(activityType)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();

        leadActivityRepository.save(activity);
    }

    private LeadResponse mapToLeadResponse(Lead lead) {
        return LeadResponse.builder()
                .id(lead.getId())
                .contactId(lead.getContactId())
                .fullName(lead.getFullName())
                .company(lead.getCompany())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .jobTitle(lead.getJobTitle())
                .industry(lead.getIndustry())
                .website(lead.getWebsite())
                .address(lead.getAddress())
                .country(lead.getCountry())
                .state(lead.getState())
                .city(lead.getCity())
                .tags(lead.getTags())
                .leadSource(lead.getLeadSource())
                .assignedOwner(lead.getAssignedOwner())
                .status(lead.getStatus())
                .pipelineStage(lead.getPipelineStage())
                .notes(lead.getNotes())
                .estimatedValue(lead.getEstimatedValue())
                .expectedCloseDate(lead.getExpectedCloseDate())
                .createdAt(lead.getCreatedAt())
                .updatedAt(lead.getUpdatedAt())
                .build();
    }

    private LeadActivityResponse mapToActivityResponse(
            LeadActivity activity
    ) {
        return LeadActivityResponse.builder()
                .id(activity.getId())
                .leadId(activity.getLeadId())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}