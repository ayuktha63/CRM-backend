package com.orque.crm.email.repository;

import com.orque.crm.email.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository
        extends JpaRepository<EmailTemplate, Long> {
}