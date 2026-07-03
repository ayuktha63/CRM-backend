package com.orque.crm.settings.smtp.repository;

import com.orque.crm.settings.smtp.entity.SmtpConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmtpConfigRepository extends JpaRepository<SmtpConfig, Long> {

    List<SmtpConfig> findAllByOwnerOrderByIsDefaultDescCreatedAtDesc(String owner);

    Optional<SmtpConfig> findByOwnerAndIsDefaultTrue(String owner);
}
