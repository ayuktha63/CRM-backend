package com.orque.crm.contact.service;

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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Override
    public ContactResponse createContact(CreateContactRequest request) {

        if (contactRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Contact with this email already exists");
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
                .status(ContactStatus.NEW)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Contact savedContact = contactRepository.save(contact);

        return mapToResponse(savedContact);
    }
    @Override
    public List<ContactResponse> getAllContacts() {

        return contactRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
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
    @Override
    public ContactResponse getContactById(Long id) {

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        return mapToResponse(contact);
    }

    @Override
    public List<ContactResponse> searchContacts(String keyword) {

        return contactRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCompanyContainingIgnoreCase(
                        keyword,
                        keyword,
                        keyword
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ContactResponse> getContactsByStatus(ContactStatus status) {

        return contactRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    @Override
    public ContactResponse updateContact(
            Long id,
            CreateContactRequest request
    ) {

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

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
        contact.setUpdatedAt(LocalDateTime.now());

        Contact updatedContact = contactRepository.save(contact);

        return mapToResponse(updatedContact);
    }

    @Override
    public void deleteContact(Long id) {

        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        contactRepository.delete(contact);
    }
    @Override
    public void updateContactStatus(
            BulkStatusUpdateRequest request
    ) {

        List<Contact> contacts =
                contactRepository.findAllById(
                        request.getContactIds()
                );

        for (Contact contact : contacts) {
            contact.setStatus(request.getStatus());
            contact.setUpdatedAt(LocalDateTime.now());
        }

        contactRepository.saveAll(contacts);
    }
    @Override
    public byte[] exportContactsAsCsv() {

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader(
                            "ID",
                            "Full Name",
                            "Company",
                            "Email",
                            "Phone",
                            "Job Title",
                            "Industry",
                            "Website",
                            "Country",
                            "State",
                            "City",
                            "Tags",
                            "Status",
                            "Notes"
                    )
                    .build());

            List<Contact> contacts = contactRepository.findAll();

            for (Contact contact : contacts) {
                csvPrinter.printRecord(
                        contact.getId(),
                        contact.getFullName(),
                        contact.getCompany(),
                        contact.getEmail(),
                        contact.getPhone(),
                        contact.getJobTitle(),
                        contact.getIndustry(),
                        contact.getWebsite(),
                        contact.getCountry(),
                        contact.getState(),
                        contact.getCity(),
                        contact.getTags(),
                        contact.getStatus(),
                        contact.getNotes()
                );
            }

            csvPrinter.flush();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export contacts");
        }
    }
}