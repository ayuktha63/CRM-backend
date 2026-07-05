package com.orque.crm.lead.service;

import com.orque.crm.lead.dto.*;

import java.util.List;

public interface LeadService {

    LeadResponse createLead(CreateLeadRequest request);

    LeadResponse convertContactToLead(
            Long contactId,
            ConvertContactToLeadRequest request
    );

    List<LeadResponse> bulkConvertContactsToLeads(
            BulkConvertContactsRequest request
    );

    List<LeadResponse> getAllLeads();

    LeadResponse getLeadById(Long id);

    List<LeadActivityResponse> getLeadActivities(Long leadId);

    LeadResponse qualifyLead(Long id);

    LeadResponse updateLead(Long id, CreateLeadRequest request);

    LeadResponse promoteToQualified(Long id);

    java.util.List<LeadResponse> bulkImportLeads(java.util.List<CreateLeadRequest> requests);

    void deleteLead(Long id);
}