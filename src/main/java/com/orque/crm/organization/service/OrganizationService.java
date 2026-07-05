package com.orque.crm.organization.service;

import com.orque.crm.organization.dto.BillingProfileRequest;
import com.orque.crm.organization.dto.OrganizationRequest;
import com.orque.crm.organization.dto.OrganizationResponse;

import java.util.List;

public interface OrganizationService {
    OrganizationResponse create(OrganizationRequest request);
    List<OrganizationResponse> listAll();
    OrganizationResponse getById(String id);
    OrganizationResponse update(String id, OrganizationRequest request);
    void suspend(String id);
    void activate(String id);

    /** Current caller's own tenant billing profile — always self-scoped, never accepts an org id. */
    OrganizationResponse getMyBillingProfile();
    OrganizationResponse updateMyBillingProfile(BillingProfileRequest request);
}
