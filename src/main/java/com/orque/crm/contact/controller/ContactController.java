package com.orque.crm.contact.controller;

import com.orque.crm.common.ApiResponse;
import com.orque.crm.contact.dto.BulkStatusUpdateRequest;
import com.orque.crm.contact.dto.ContactResponse;
import com.orque.crm.contact.dto.CreateContactRequest;
import com.orque.crm.contact.service.ContactService;
import com.orque.crm.enums.ContactStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ContactResponse> createContact(
            @Valid @RequestBody CreateContactRequest request
    ) {
        ContactResponse response =
                contactService.createContact(request);

        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<ContactResponse>> getAllContacts() {

        List<ContactResponse> response =
                contactService.getAllContacts();

        return ResponseEntity.ok(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ContactResponse> getContactById(
            @PathVariable Long id
    ) {
        ContactResponse response =
                contactService.getContactById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ContactResponse>> searchContacts(
            @RequestParam String keyword
    ) {
        List<ContactResponse> response =
                contactService.searchContacts(keyword);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ContactResponse>> getContactsByStatus(
            @PathVariable ContactStatus status
    ) {
        List<ContactResponse> response =
                contactService.getContactsByStatus(status);

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    public ResponseEntity<ContactResponse> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody CreateContactRequest request
    ) {
        ContactResponse response =
                contactService.updateContact(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteContact(
            @PathVariable Long id
    ) {
        contactService.deleteContact(id);

        return ResponseEntity.ok(
                new ApiResponse(true, "Contact deleted successfully")
        );
    }
    @PutMapping("/bulk-status")
    public ResponseEntity<ApiResponse> updateContactStatus(
            @Valid
            @RequestBody
            BulkStatusUpdateRequest request
    ) {

        contactService.updateContactStatus(request);

        return ResponseEntity.ok(
                new ApiResponse(
                        true,
                        "Contacts updated successfully"
                )
        );
    }
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportContacts() {

        byte[] csvData = contactService.exportContactsAsCsv();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=contacts.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}