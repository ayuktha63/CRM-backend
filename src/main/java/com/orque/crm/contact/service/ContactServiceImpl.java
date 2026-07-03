package com.orque.crm.contact.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.contact.dto.BulkStatusUpdateRequest;
import com.orque.crm.contact.dto.ContactResponse;
import com.orque.crm.contact.dto.CreateContactRequest;
import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.enums.ContactStatus;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private static final String CONTACT_NOT_FOUND = "Contact not found";

    private final ContactRepository contactRepository;

    @Override
    public ContactResponse createContact(CreateContactRequest request) {
        if (contactRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Contact with this email already exists");
        }

        Contact contact = Contact.builder()
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
                .notes(request.getNotes())
                .status(request.getStatus() != null ? request.getStatus() : ContactStatus.NEW)
                .owner(UserContextHelper.currentUsername())
                .organizationId(UserContextHelper.currentOrganizationId())
                .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
                .build();

        Contact saved = contactRepository.save(contact);
        log.info("Contact saved: id={}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public List<ContactResponse> getAllContacts() {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        if (orgId == null) {
            return contactRepository.findAll().stream().map(this::mapToResponse).toList();
        }
        if (owner == null) {
            return contactRepository.findByOrganizationId(orgId).stream().map(this::mapToResponse).toList();
        }
        return contactRepository.findByOrganizationIdAndOwner(orgId, owner).stream().map(this::mapToResponse).toList();
    }

    @Override
    public ContactResponse getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CONTACT_NOT_FOUND));
        UserContextHelper.assertAccess(contact.getOrganizationId(), contact.getOwner());
        return mapToResponse(contact);
    }

    @Override
    public List<ContactResponse> searchContacts(String keyword) {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        if (orgId == null) {
            // SYSTEM_ADMIN: search across all orgs
            return contactRepository
                    .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCompanyContainingIgnoreCase(
                            keyword, keyword, keyword)
                    .stream().map(this::mapToResponse).toList();
        }
        // DB-level org filter — no cross-tenant data loaded
        return contactRepository.searchByKeywordAndOrg(keyword, orgId)
                .stream()
                .filter(c -> owner == null || UserContextHelper.canAccess(c.getOwner()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ContactResponse> getContactsByStatus(ContactStatus status) {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        if (orgId == null) {
            return contactRepository.findByStatus(status).stream().map(this::mapToResponse).toList();
        }
        if (owner == null) {
            return contactRepository.findByStatusAndOrganizationId(status, orgId)
                    .stream().map(this::mapToResponse).toList();
        }
        return contactRepository.findByStatusAndOrganizationIdAndOwner(status, orgId, owner)
                .stream().map(this::mapToResponse).toList();
    }

    @Override
    public ContactResponse updateContact(Long id, CreateContactRequest request) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CONTACT_NOT_FOUND));
        UserContextHelper.assertAccess(contact.getOwner());

        contact.setFullName(request.getFullName());
        contact.setCompany(request.getCompany());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setJobTitle(request.getJobTitle());
        contact.setIndustry(request.getIndustry());
        contact.setWebsite(request.getWebsite());
        contact.setAddress(request.getAddress());
        contact.setCountry(request.getCountry());
        contact.setState(request.getState());
        contact.setCity(request.getCity());
        contact.setTags(request.getTags());
        contact.setNotes(request.getNotes());
        if (request.getStatus() != null) {
            contact.setStatus(request.getStatus());
        }
        contact.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));

        return mapToResponse(contactRepository.save(contact));
    }

    @Override
    public void deleteContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CONTACT_NOT_FOUND));
        UserContextHelper.assertAccess(contact.getOrganizationId(), contact.getOwner());
        contactRepository.delete(contact);
    }

    @Override
    public void updateContactStatus(BulkStatusUpdateRequest request) {
        String orgId = UserContextHelper.scopedOrgId();
        List<Contact> contacts = contactRepository.findAllById(request.getContactIds());
        for (Contact contact : contacts) {
            if (orgId != null && !orgId.equals(contact.getOrganizationId())) continue;
            contact.setStatus(request.getStatus());
            contact.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        }
        contactRepository.saveAll(contacts);
    }

    @Override
    public byte[] exportContactsAsCsv() {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("ID", "Full Name", "Company", "Email", "Phone",
                            "Job Title", "Industry", "Website", "Country",
                            "State", "City", "Tags", "Status", "Notes")
                    .build());

            List<Contact> allContacts = orgId != null
                    ? contactRepository.findByOrganizationId(orgId)
                    : contactRepository.findAll();
            if (owner != null) {
                allContacts = allContacts.stream()
                        .filter(c -> owner.equalsIgnoreCase(c.getOwner()))
                        .toList();
            }
            for (Contact contact : allContacts) {
                csvPrinter.printRecord(
                        contact.getId(), contact.getFullName(), contact.getCompany(),
                        contact.getEmail(), contact.getPhone(), contact.getJobTitle(),
                        contact.getIndustry(), contact.getWebsite(), contact.getCountry(),
                        contact.getState(), contact.getCity(), contact.getTags(),
                        contact.getStatus(), contact.getNotes()
                );
            }

            csvPrinter.flush();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to export contacts", e);
        }
    }

    private ContactResponse mapToResponse(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
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
                .notes(contact.getNotes())
                .status(contact.getStatus())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }
}
