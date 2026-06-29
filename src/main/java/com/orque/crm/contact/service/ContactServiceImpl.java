package com.orque.crm.contact.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.contact.dto.BulkStatusUpdateRequest;
import com.orque.crm.contact.dto.ContactResponse;
import com.orque.crm.contact.dto.CreateContactRequest;
import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.enums.ContactStatus;
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
                .createdAt(LocalDateTime.now(ZoneId.systemDefault()))
                .updatedAt(LocalDateTime.now(ZoneId.systemDefault()))
                .build();

        return mapToResponse(contactRepository.save(contact));
    }

    @Override
    public List<ContactResponse> getAllContacts() {
        return contactRepository.findAll().stream()
                .filter(c -> UserContextHelper.canAccess(c.getOwner()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public ContactResponse getContactById(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(CONTACT_NOT_FOUND));
        UserContextHelper.assertAccess(contact.getOwner());
        return mapToResponse(contact);
    }

    @Override
    public List<ContactResponse> searchContacts(String keyword) {
        return contactRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCompanyContainingIgnoreCase(
                        keyword, keyword, keyword)
                .stream()
                .filter(c -> UserContextHelper.canAccess(c.getOwner()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ContactResponse> getContactsByStatus(ContactStatus status) {
        return contactRepository.findByStatus(status).stream()
                .filter(c -> UserContextHelper.canAccess(c.getOwner()))
                .map(this::mapToResponse)
                .toList();
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
        contactRepository.delete(contact);
    }

    @Override
    public void updateContactStatus(BulkStatusUpdateRequest request) {
        List<Contact> contacts = contactRepository.findAllById(request.getContactIds());
        for (Contact contact : contacts) {
            contact.setStatus(request.getStatus());
            contact.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        }
        contactRepository.saveAll(contacts);
    }

    @Override
    public byte[] exportContactsAsCsv() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("ID", "Full Name", "Company", "Email", "Phone",
                            "Job Title", "Industry", "Website", "Country",
                            "State", "City", "Tags", "Status", "Notes")
                    .build());

            for (Contact contact : contactRepository.findAll()) {
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
