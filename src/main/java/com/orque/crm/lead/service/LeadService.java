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
}