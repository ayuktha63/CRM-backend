package com.orque.crm.contact.repository;

import com.orque.crm.contact.entity.Contact;
import com.orque.crm.enums.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    boolean existsByEmail(String email);
    java.util.List<com.orque.crm.contact.entity.Contact> findByOrganizationId(String organizationId);
    java.util.List<com.orque.crm.contact.entity.Contact> findByOrganizationIdAndOwner(String organizationId, String owner);

    java.util.Optional<Contact> findByEmail(String email);

    java.util.Optional<Contact> findByEmailIgnoreCase(String email);

    java.util.List<Contact> findByCompanyIgnoreCase(String company);

    List<Contact> findByStatus(ContactStatus status);

    List<Contact> findByStatusAndOrganizationId(ContactStatus status, String organizationId);

    List<Contact> findByStatusAndOrganizationIdAndOwner(ContactStatus status, String organizationId, String owner);

    List<Contact> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrCompanyContainingIgnoreCase(
            String fullName,
            String email,
            String company
    );

    @Query("SELECT c FROM Contact c WHERE c.organizationId = :orgId AND " +
           "(LOWER(c.fullName) LIKE LOWER(CONCAT('%',:kw,'%')) OR " +
           " LOWER(c.email)    LIKE LOWER(CONCAT('%',:kw,'%')) OR " +
           " LOWER(c.company)  LIKE LOWER(CONCAT('%',:kw,'%')))")
    List<Contact> searchByKeywordAndOrg(@Param("kw") String keyword, @Param("orgId") String orgId);
}