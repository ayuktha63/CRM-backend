package com.orque.crm.organization.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * Tenant self-service billing details — printed on that tenant's own Quote/Invoice
 * PDFs. Deliberately separate from OrganizationRequest (which requires
 * organizationCode/organizationName and is reserved for platform-level org
 * management) since a tenant's own SYSTEM_ADMIN should only ever be able to edit
 * their own billing info, never re-provision the organization itself.
 */
@Data
public class BillingProfileRequest {

    private String legalName;

    @Email
    private String email;

    private String phone;
    private String address;
    private String gstin;
    private String companyTagline;
    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String upiId;
    private Integer paymentTermsDays;
    private String lateFeeText;
}
