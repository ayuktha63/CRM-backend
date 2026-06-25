package com.orque.crm.email.repository;

import com.orque.crm.email.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailMessageRepository
        extends JpaRepository<EmailMessage, Long> {

    List<EmailMessage> findByLeadId(Long leadId);

    List<EmailMessage> findByContactId(Long contactId);

    List<EmailMessage> findByGmailThreadId(String threadId);
}