package com.orque.crm.license.service;

import com.orque.crm.license.dto.LicenseActivationRequest;
import com.orque.crm.license.dto.LicenseGenerateRequest;
import com.orque.crm.license.dto.LicenseStatusResponse;

public interface LicenseService {

    /** Activate (or replace) a license for the given organization. SYSTEM_ADMIN only. */
    LicenseStatusResponse activate(LicenseActivationRequest request);

    /** Get the current license status for an organization. */
    LicenseStatusResponse getStatus(String organizationId);

    /** Compute the effective status for the current request (used by the security filter). */
    LicenseCheckResult check(String organizationId);

    /** Generate an encrypted license key string for a new license. SYSTEM_ADMIN only. */
    String generateKey(LicenseGenerateRequest request);

    record LicenseCheckResult(boolean allowed, boolean inGrace, int graceRemaining, String message, int concurrentLimit) {}
}
