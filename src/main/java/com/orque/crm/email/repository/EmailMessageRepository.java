package com.orque.crm.email.repository;

import com.orque.crm.email.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailMessageRepository
        extends JpaRepository<EmailMessage, Long> {

    List<EmailMessage> findByLeadId(Long leadId);

    List<EmailMessage> findByContactId(Long contactId);

    List<EmailMessage> findByGmailThreadId(String threadId);

    List<EmailMessage> findByFromEmailOrderBySentAtDesc(String fromEmail);

    List<EmailMessage> findByFolderOrderBySentAtDesc(String folder);

    @Query("SELECT e FROM EmailMessage e WHERE e.folder = :folder AND (e.toEmail = :email OR e.fromEmail = :email) ORDER BY e.sentAt DESC")
    Page<EmailMessage> findByFolderAndUserEmail(@Param("folder") String folder, @Param("email") String email, Pageable pageable);

    Page<EmailMessage> findByFolderOrderBySentAtDesc(String folder, Pageable pageable);
}