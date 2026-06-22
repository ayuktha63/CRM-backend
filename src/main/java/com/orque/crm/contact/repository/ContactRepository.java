package com.orque.crm.contact.repository;

import com.orque.crm.contact.entity.Contact;
import com.orque.crm.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByEmail(String email);

    List<Contact> findByStatus(ContactStatus status);

    List<Contact> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCompanyContainingIgnoreCase(
            String fullName,
            String email,
            String company
    );
}