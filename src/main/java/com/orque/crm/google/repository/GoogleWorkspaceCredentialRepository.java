package com.orque.crm.google.repository;

import com.orque.crm.google.entity.GoogleWorkspaceCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoogleWorkspaceCredentialRepository extends JpaRepository<GoogleWorkspaceCredential, Long> {
    Optional<GoogleWorkspaceCredential> findByOwnerIgnoreCase(String owner);
    List<GoogleWorkspaceCredential> findByConnectedTrueAndRevokedFalse();
}
