package com.orque.crm.contact.service;

import com.orque.crm.contact.dto.BulkStatusUpdateRequest;
import com.orque.crm.contact.dto.ContactResponse;
import com.orque.crm.contact.dto.CreateContactRequest;
import com.orque.crm.enums.ContactStatus;

import java.util.List;

public interface ContactService {

    ContactResponse createContact(
            CreateContactRequest request);

    List<ContactResponse> getAllContacts();
    ContactResponse getContactById(Long id);
    List<ContactResponse> searchContacts(String keyword);
    List<ContactResponse> getContactsByStatus(ContactStatus status);
    ContactResponse updateContact(
            Long id,
            CreateContactRequest request
    );

    void deleteContact(Long id);
    void updateContactStatus(BulkStatusUpdateRequest request);
    byte[] exportContactsAsCsv();
}