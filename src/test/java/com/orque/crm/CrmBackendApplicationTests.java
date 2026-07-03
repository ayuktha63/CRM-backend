package com.orque.crm;

import com.orque.crm.license.util.LicenseEncryptionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CrmBackendApplicationTests {

    @Autowired
    private LicenseEncryptionUtil encryptionUtil;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.orque.crm.license.service.LicenseService licenseService;

    @Autowired
    private com.orque.crm.license.repository.CrmLicenseRepository licenseRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void generateTestLicenseKey() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // 1. SYSTEM License Key
        LicenseEncryptionUtil.OpacProduct systemProduct = LicenseEncryptionUtil.OpacProduct.builder()
                .productName("CRM")
                .startDate("2026-07-01")
                .endDate("2027-07-01")
                .userLimit(10)
                .concurrentLimit(2)
                .gracePeriod(30)
                .features(java.util.Arrays.asList(
                    "/dashboard", "/leads", "/contacts", "/accounts", "/deals", 
                    "/activities", "/tasks", "/calendar", "/campaigns", "/emails", 
                    "/products", "/quotes", "/invoices", "/reports", "/analytics", 
                    "/settings", "/users", "/active-sessions"
                ))
                .build();

        LicenseEncryptionUtil.OpacTenant systemTenant = LicenseEncryptionUtil.OpacTenant.builder()
                .tenantName("system")
                .company("System")
                .build();

        String systemProdsStr = mapper.writeValueAsString(java.util.Collections.singletonList(systemProduct));
        String systemSig = encryptionUtil.generateHmac(systemProdsStr);

        LicenseEncryptionUtil.OpacLicensePayload systemPayload = LicenseEncryptionUtil.OpacLicensePayload.builder()
                .licenseVersion("1.0")
                .issueDate("2026-07-01")
                .expiryDate("2027-07-01")
                .licenseType("Standard")
                .tenant(systemTenant)
                .products(java.util.Collections.singletonList(systemProduct))
                .digitalSignature(systemSig)
                .build();

        String systemKey = encryptionUtil.encryptHex(mapper.writeValueAsString(systemPayload));

        // 2. DEFAULT Org License Key
        LicenseEncryptionUtil.OpacProduct defaultProduct = LicenseEncryptionUtil.OpacProduct.builder()
                .productName("CRM")
                .startDate("2026-07-01")
                .endDate("2027-07-01")
                .userLimit(10)
                .concurrentLimit(5)
                .gracePeriod(30)
                .features(java.util.Arrays.asList(
                    "/dashboard", "/leads", "/contacts", "/accounts", "/deals", 
                    "/activities", "/tasks", "/calendar", "/campaigns", "/emails", 
                    "/products", "/quotes", "/invoices", "/reports", "/analytics", 
                    "/settings", "/users", "/active-sessions"
                ))
                .build();

        LicenseEncryptionUtil.OpacTenant defaultTenant = LicenseEncryptionUtil.OpacTenant.builder()
                .tenantName("default")
                .company("Default Organization")
                .build();

        String defaultProdsStr = mapper.writeValueAsString(java.util.Collections.singletonList(defaultProduct));
        String defaultSig = encryptionUtil.generateHmac(defaultProdsStr);

        LicenseEncryptionUtil.OpacLicensePayload defaultPayload = LicenseEncryptionUtil.OpacLicensePayload.builder()
                .licenseVersion("1.0")
                .issueDate("2026-07-01")
                .expiryDate("2027-07-01")
                .licenseType("Standard")
                .tenant(defaultTenant)
                .products(java.util.Collections.singletonList(defaultProduct))
                .digitalSignature(defaultSig)
                .build();

        String defaultKey = encryptionUtil.encryptHex(mapper.writeValueAsString(defaultPayload));

        // 3. RESTRICTED Org License Key
        LicenseEncryptionUtil.OpacProduct restrictedProduct = LicenseEncryptionUtil.OpacProduct.builder()
                .productName("CRM")
                .startDate("2026-07-01")
                .endDate("2027-07-01")
                .userLimit(10)
                .concurrentLimit(5)
                .gracePeriod(30)
                .features(java.util.Arrays.asList(
                    "/dashboard", "/leads", "/contacts", "/settings"
                ))
                .build();

        LicenseEncryptionUtil.OpacTenant restrictedTenant = LicenseEncryptionUtil.OpacTenant.builder()
                .tenantName("default")
                .company("Default Organization")
                .build();

        String restrictedProdsStr = mapper.writeValueAsString(java.util.Collections.singletonList(restrictedProduct));
        String restrictedSig = encryptionUtil.generateHmac(restrictedProdsStr);

        LicenseEncryptionUtil.OpacLicensePayload restrictedPayload = LicenseEncryptionUtil.OpacLicensePayload.builder()
                .licenseVersion("1.0")
                .issueDate("2026-07-01")
                .expiryDate("2027-07-01")
                .licenseType("Standard")
                .tenant(restrictedTenant)
                .products(java.util.Collections.singletonList(restrictedProduct))
                .digitalSignature(restrictedSig)
                .build();

        String restrictedKey = encryptionUtil.encryptHex(mapper.writeValueAsString(restrictedPayload));

        System.out.println("=================================================================");
        System.out.println("GENERATED TEST LICENSE KEY FOR SYSTEM:");
        System.out.println(systemKey);
        System.out.println("GENERATED TEST LICENSE KEY FOR DEFAULT ORGANIZATION:");
        System.out.println(defaultKey);
        System.out.println("GENERATED TEST LICENSE KEY FOR RESTRICTED DEFAULT ORG:");
        System.out.println(restrictedKey);
        System.out.println("BCRYPT HASH FOR Orque@123!Test: " + passwordEncoder.encode("Orque@123!Test"));
        System.out.println("=================================================================");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void testCheckLicenseDateRange() {
        // Clear any existing test licenses
        licenseRepository.findByOrganizationId("TEST-ORG-DATE")
                .ifPresent(licenseRepository::delete);

        // 1. License with future start date
        com.orque.crm.license.entity.CrmLicense futureLicense = com.orque.crm.license.entity.CrmLicense.builder()
                .organizationId("TEST-ORG-DATE")
                .licenseName("Future License")
                .orgCode("TEST")
                .productName("CRM")
                .startDate(java.time.LocalDate.now().plusDays(2))
                .endDate(java.time.LocalDate.now().plusDays(10))
                .gracePeriodDays(30)
                .licenseKey("dummy-key")
                .licenseHash("dummy-hash")
                .status(com.orque.crm.enums.LicenseStatus.ACTIVE)
                .build();
        licenseRepository.save(futureLicense);

        com.orque.crm.license.service.LicenseService.LicenseCheckResult result = licenseService.check("TEST-ORG-DATE");
        org.junit.jupiter.api.Assertions.assertFalse(result.allowed());
        org.junit.jupiter.api.Assertions.assertTrue(result.message().contains("not active yet"));

        // 2. Active license (today is within range)
        futureLicense.setStartDate(java.time.LocalDate.now().minusDays(1));
        licenseRepository.save(futureLicense);

        result = licenseService.check("TEST-ORG-DATE");
        org.junit.jupiter.api.Assertions.assertTrue(result.allowed());

        // Cleanup
        licenseRepository.findByOrganizationId("TEST-ORG-DATE")
                .ifPresent(licenseRepository::delete);
    }
}
