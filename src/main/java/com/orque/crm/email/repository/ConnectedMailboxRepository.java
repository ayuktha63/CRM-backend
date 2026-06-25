package com.orque.crm.email.repository;

import com.orque.crm.email.entity.ConnectedMailbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConnectedMailboxRepository
        extends JpaRepository<ConnectedMailbox, Long> {

    Optional<ConnectedMailbox> findByEmailAddress(
            String emailAddress
    );
}