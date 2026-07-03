package com.orque.crm.contact.controller;

import com.orque.crm.common.ApiResponse;
import com.orque.crm.config.query.QueryMapping;
import com.orque.crm.contact.dto.BulkStatusUpdateRequest;
import com.orque.crm.contact.dto.ContactResponse;
import com.orque.crm.contact.dto.ContactSearchRequest;
import com.orque.crm.contact.dto.CreateContactRequest;
import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.contact.service.ContactService;
import com.orque.crm.enums.ContactStatus;
import com.orque.crm.feature.entity.Activity;
import com.orque.crm.feature.entity.Deal;
import com.orque.crm.feature.entity.Invoice;
import com.orque.crm.feature.entity.Quote;
import com.orque.crm.feature.repository.ActivityRepository;
import com.orque.crm.feature.repository.DealRepository;
import com.orque.crm.feature.repository.InvoiceRepository;
import com.orque.crm.feature.repository.QuoteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@CrossOrigin
public class ContactController {

    private final ContactService contactService;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final ActivityRepository activityRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;

    @PostMapping
    public ResponseEntity<ContactResponse> createContact(
            @Valid @RequestBody CreateContactRequest request) {
        return ResponseEntity.ok(contactService.createContact(request));
    }

    @GetMapping
    public ResponseEntity<List<ContactResponse>> getAllContacts() {
        return ResponseEntity.ok(contactService.getAllContacts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getContactById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getContactById(id));
    }

    /**
     * HTTP QUERY /api/v1/contacts/search (RFC 10008) — safe, idempotent contact search.
     * Body: { "keyword": "..." }
     */
    @QueryMapping("/search")
    public ResponseEntity<List<ContactResponse>> searchContacts(@RequestBody ContactSearchRequest request) {
        return ResponseEntity.ok(contactService.searchContacts(request.getKeyword()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ContactResponse>> getContactsByStatus(@PathVariable ContactStatus status) {
        return ResponseEntity.ok(contactService.getContactsByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody CreateContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.ok(new ApiResponse(true, "Contact deleted successfully"));
    }

    @PutMapping("/bulk-status")
    public ResponseEntity<ApiResponse> updateContactStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request) {
        contactService.updateContactStatus(request);
        return ResponseEntity.ok(new ApiResponse(true, "Contacts updated successfully"));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportContacts() {
        byte[] csvData = contactService.exportContactsAsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contacts.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    // ── Related records ──────────────────────────────────────────────────────

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<Activity>> getActivities(@PathVariable Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        // Match by linked relatedId
        List<Activity> byId = activityRepository.findByRelatedTypeIgnoreCaseAndRelatedId("Contact", id);
        // Match by contact name (exact and containing)
        List<Activity> byName = activityRepository.findByContactContainingIgnoreCase(contact.getFullName());
        return ResponseEntity.ok(
                Stream.concat(byId.stream(), byName.stream())
                      .distinct()
                      .sorted(java.util.Comparator.comparing(
                          a -> a.getCreatedAt() == null ? java.time.LocalDateTime.MIN : a.getCreatedAt(),
                          java.util.Comparator.reverseOrder()))
                      .toList());
    }

    @GetMapping("/{id}/deals")
    public ResponseEntity<List<Deal>> getDeals(@PathVariable Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        return ResponseEntity.ok(dealRepository.findByContactIgnoreCase(contact.getFullName()));
    }

    @GetMapping("/{id}/quotes")
    public ResponseEntity<List<Quote>> getQuotes(@PathVariable Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        return ResponseEntity.ok(quoteRepository.findByContactIgnoreCase(contact.getFullName()));
    }

    @GetMapping("/{id}/invoices")
    public ResponseEntity<List<Invoice>> getInvoices(@PathVariable Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        return ResponseEntity.ok(invoiceRepository.findByContactIgnoreCase(contact.getFullName()));
    }
}
