package com.orque.crm.lead.service;

import com.orque.crm.audit.service.AuditLogService;
import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.enums.*;
import com.orque.crm.feature.entity.Account;
import com.orque.crm.feature.entity.Deal;
import com.orque.crm.feature.repository.AccountRepository;
import com.orque.crm.feature.repository.DealRepository;
import com.orque.crm.lead.dto.*;
import com.orque.crm.lead.entity.Lead;
import com.orque.crm.lead.entity.LeadActivity;
import com.orque.crm.lead.repository.LeadActivityRepository;
import com.orque.crm.lead.repository.LeadRepository;
import com.orque.crm.note.entity.Note;
import com.orque.crm.note.repository.NoteRepository;
import com.orque.crm.attachment.entity.Attachment;
import com.orque.crm.attachment.repository.AttachmentRepository;
import com.orque.crm.task.entity.CrmTask;
import com.orque.crm.task.repository.CrmTaskRepository;
import com.orque.crm.feature.entity.Activity;
import com.orque.crm.feature.repository.ActivityRepository;
import com.orque.crm.email.entity.EmailMessage;
import com.orque.crm.email.repository.EmailMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.orque.crm.enums.AuditModule;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository leadActivityRepository;
    private final ContactRepository contactRepository;
    private final AccountRepository accountRepository;
    private final DealRepository dealRepository;
    private final AuditLogService auditLogService;
    private final NoteRepository noteRepository;
    private final AttachmentRepository attachmentRepository;
    private final ActivityRepository activityRepository;
    private final CrmTaskRepository crmTaskRepository;
    private final EmailMessageRepository emailMessageRepository;

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

    @Override
    @Transactional
    public LeadResponse qualifyLead(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found: " + id));

        if (lead.getStatus() == LeadStatus.CONVERTED) {
            throw new RuntimeException("Lead has already been converted.");
        }
        if (lead.getStatus() != LeadStatus.QUALIFIED) {
            throw new RuntimeException("Only QUALIFIED leads can be converted. Please qualify the lead first.");
        }

        // 1. Create or reuse Contact (case-insensitive email match)
        Contact contact = contactRepository.findByEmailIgnoreCase(lead.getEmail())
                .orElseGet(() -> contactRepository.save(
                        Contact.builder()
                                .fullName(lead.getFullName())
                                .email(lead.getEmail())
                                .phone(lead.getPhone())
                                .company(lead.getCompany())
                                .jobTitle(lead.getJobTitle())
                                .industry(lead.getIndustry())
                                .website(lead.getWebsite())
                                .address(lead.getAddress())
                                .country(lead.getCountry())
                                .state(lead.getState())
                                .city(lead.getCity())
                                .tags(lead.getTags())
                                .status(ContactStatus.NEW)
                                .owner(lead.getAssignedOwner())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                ));

        // 2. Create or reuse Account (match by company name)
        final Account account = (lead.getCompany() != null && !lead.getCompany().isBlank())
                ? accountRepository.findByCompanyNameIgnoreCase(lead.getCompany())
                        .orElseGet(() -> accountRepository.save(
                                Account.builder()
                                        .companyName(lead.getCompany())
                                        .industry(lead.getIndustry())
                                        .phone(lead.getPhone())
                                        .website(lead.getWebsite())
                                        .country(lead.getCountry())
                                        .status("Active")
                                        .owner(lead.getAssignedOwner())
                                        .build()
                        ))
                : null;

        // 3. Create Deal from lead data
        Deal deal = dealRepository.save(
                Deal.builder()
                        .dealName(lead.getFullName() + " — Deal")
                        .account(account != null ? account.getCompanyName() : "")
                        .contact(lead.getFullName())
                        .amount(lead.getEstimatedValue())
                        .stage("Prospecting")
                        .expectedCloseDate(lead.getExpectedCloseDate())
                        .assignedTo(lead.getAssignedOwner())
                        .build()
        );

        // 4. Mark Lead as CONVERTED
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setContactId(contact.getId());
        lead.setUpdatedAt(LocalDateTime.now());
        Lead saved = leadRepository.save(lead);

        // Migrate related records (notes, tasks, activities, email history, attachments)
        migrateLeadRelatedRecords(saved, contact, account);

        createLeadActivity(saved.getId(), LeadActivityType.LEAD_CONVERTED,
                "Lead converted: Contact #" + contact.getId()
                + (account != null ? ", Account #" + account.getId() : "")
                + ", Deal #" + deal.getId());

        auditLogService.createAudit(
                AuditAction.LEAD_CONVERTED,
                AuditModule.LEAD,
                "Lead",
                saved.getId(),
                LeadStatus.QUALIFIED.name(),
                LeadStatus.CONVERTED.name(),
                "Lead converted — Contact, Account and Deal created/linked",
                saved.getAssignedOwner(),
                null
        );

        return mapToLeadResponse(saved);
    }

    private void migrateLeadRelatedRecords(Lead lead, Contact contact, Account account) {
        Long leadId = lead.getId();

        // 1. Migrate Notes (clone to Contact and Account)
        List<Note> leadNotes = noteRepository.findByModuleNameAndRecordIdOrderByCreatedAtDesc("leads", leadId);
        for (Note note : leadNotes) {
            noteRepository.save(Note.builder()
                    .moduleName("contacts")
                    .recordId(contact.getId())
                    .content(note.getContent())
                    .createdBy(note.getCreatedBy())
                    .build());
            
            if (account != null) {
                noteRepository.save(Note.builder()
                        .moduleName("accounts")
                        .recordId(account.getId())
                        .content(note.getContent())
                        .createdBy(note.getCreatedBy())
                        .build());
            }
        }

        // 2. Migrate Attachments (clone to Contact and Account)
        List<Attachment> leadAttachments = attachmentRepository.findByModuleNameAndRecordIdOrderByCreatedAtDesc("leads", leadId);
        for (Attachment att : leadAttachments) {
            attachmentRepository.save(Attachment.builder()
                    .moduleName("contacts")
                    .recordId(contact.getId())
                    .fileName(att.getFileName())
                    .fileSize(att.getFileSize())
                    .contentType(att.getContentType())
                    .fileUrl(att.getFileUrl())
                    .version(att.getVersion())
                    .createdBy(att.getCreatedBy())
                    .build());
            
            if (account != null) {
                attachmentRepository.save(Attachment.builder()
                        .moduleName("accounts")
                        .recordId(account.getId())
                        .fileName(att.getFileName())
                        .fileSize(att.getFileSize())
                        .contentType(att.getContentType())
                        .fileUrl(att.getFileUrl())
                        .version(att.getVersion())
                        .createdBy(att.getCreatedBy())
                        .build());
            }
        }

        // 3. Migrate Activities (re-associate to Contact)
        List<Activity> leadActivities = activityRepository.findByRelatedTypeIgnoreCaseAndRelatedId("Lead", leadId);
        for (Activity act : leadActivities) {
            act.setRelatedType("Contact");
            act.setRelatedId(contact.getId());
            act.setContact(contact.getFullName());
            activityRepository.save(act);
        }

        // 4. Migrate Tasks (re-associate to Contact)
        List<CrmTask> leadTasks = crmTaskRepository.findByLeadId(leadId);
        for (CrmTask task : leadTasks) {
            task.setContactId(contact.getId());
            task.setRelatedType("Contact");
            task.setRelatedId(contact.getId());
            crmTaskRepository.save(task);
        }

        // 5. Migrate Emails (re-associate to Contact)
        List<EmailMessage> leadEmails = emailMessageRepository.findByLeadId(leadId);
        for (EmailMessage email : leadEmails) {
            email.setContactId(contact.getId());
            emailMessageRepository.save(email);
        }
    }

    @Override
    public LeadResponse updateLead(Long id, CreateLeadRequest request) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found: " + id));

        if (!lead.getEmail().equalsIgnoreCase(request.getEmail())
                && leadRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Another lead with this email already exists");
        }

        lead.setFullName(request.getFullName());
        lead.setCompany(request.getCompany());
        lead.setEmail(request.getEmail());
        lead.setPhone(request.getPhone());
        lead.setJobTitle(request.getJobTitle());
        lead.setIndustry(request.getIndustry());
        lead.setWebsite(request.getWebsite());
        lead.setAddress(request.getAddress());
        lead.setCountry(request.getCountry());
        lead.setState(request.getState());
        lead.setCity(request.getCity());
        lead.setTags(request.getTags());
        if (request.getLeadSource() != null) lead.setLeadSource(request.getLeadSource());
        lead.setAssignedOwner(request.getAssignedOwner());
        if (request.getStatus() != null) lead.setStatus(request.getStatus());
        lead.setNotes(request.getNotes());
        lead.setEstimatedValue(request.getEstimatedValue());
        lead.setExpectedCloseDate(request.getExpectedCloseDate());
        lead.setUpdatedAt(LocalDateTime.now());

        Lead saved = leadRepository.save(lead);
        auditLogService.createAudit(AuditAction.LEAD_STATUS_CHANGED, AuditModule.LEAD, "Lead",
                saved.getId(), null, saved.getFullName(), "Lead updated", saved.getAssignedOwner(), null);
        return mapToLeadResponse(saved);
    }

    @Override
    public LeadResponse promoteToQualified(Long id) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lead not found: " + id));

        lead.setStatus(LeadStatus.QUALIFIED);
        lead.setUpdatedAt(LocalDateTime.now());
        Lead saved = leadRepository.save(lead);

        createLeadActivity(saved.getId(), LeadActivityType.STATUS_CHANGED, "Lead promoted to QUALIFIED");
        auditLogService.createAudit(AuditAction.LEAD_STATUS_CHANGED, AuditModule.LEAD, "Lead",
                saved.getId(), LeadStatus.NEW.name(), LeadStatus.QUALIFIED.name(),
                "Lead status changed to QUALIFIED", saved.getAssignedOwner(), null);
        return mapToLeadResponse(saved);
    }

    @Override
    public java.util.List<LeadResponse> bulkImportLeads(java.util.List<CreateLeadRequest> requests) {
        java.util.List<LeadResponse> imported = new java.util.ArrayList<>();
        for (CreateLeadRequest req : requests) {
            if (req.getEmail() == null || req.getEmail().isBlank()) continue;
            if (leadRepository.existsByEmail(req.getEmail())) continue;
            try { imported.add(createLead(req)); } catch (RuntimeException ignored) { /* skip */ }
        }
        return imported;
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