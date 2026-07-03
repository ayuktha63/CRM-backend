package com.orque.crm.lead.repository;

import com.orque.crm.enums.LeadStatus;
import com.orque.crm.enums.PipelineStage;
import com.orque.crm.lead.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    boolean existsByEmail(String email);

    boolean existsByContactId(Long contactId);

    List<Lead> findByStatus(LeadStatus status);
    List<Lead> findByAssignedOwner(String assignedOwner);
    List<Lead> findByAssignedOwnerContainingIgnoreCase(String assignedOwner);
    List<Lead> findByPipelineStage(PipelineStage pipelineStage);

    List<Lead> findByOrganizationId(String organizationId);
    List<Lead> findByOrganizationIdAndAssignedOwner(String organizationId, String assignedOwner);
    List<Lead> findByOrganizationIdAndStatus(String organizationId, LeadStatus status);
    List<Lead> findByOrganizationIdAndPipelineStage(String organizationId, PipelineStage pipelineStage);
}