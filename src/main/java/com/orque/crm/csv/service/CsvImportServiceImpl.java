package com.orque.crm.csv.service;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.csv.dto.CsvImportError;
import com.orque.crm.csv.dto.CsvImportMappingRequest;
import com.orque.crm.csv.dto.CsvImportResponse;
import com.orque.crm.csv.dto.CsvPreviewResponse;
import com.orque.crm.csv.entity.CsvImportHistory;
import com.orque.crm.csv.repository.CsvImportHistoryRepository;
import com.orque.crm.enums.ContactStatus;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.*;
import com.orque.crm.audit.service.AuditLogService;
import com.orque.crm.enums.AuditAction;
import com.orque.crm.enums.AuditModule;
@Service
@RequiredArgsConstructor
public class CsvImportServiceImpl implements CsvImportService {

    private final ContactRepository contactRepository;
    private final CsvImportHistoryRepository csvImportHistoryRepository;
    private final AuditLogService auditLogService;

    @Override
    public CsvPreviewResponse previewCsv(MultipartFile file) {

        validateCsvFile(file);

        try {
            Reader reader = new InputStreamReader(file.getInputStream());

            CSVParser csvParser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            List<String> detectedColumns = new ArrayList<>(csvParser.getHeaderMap().keySet());

            List<Map<String, String>> previewRows = new ArrayList<>();

            int rowLimit = 5;
            int count = 0;

            for (CSVRecord record : csvParser) {

                if (count >= rowLimit) {
                    break;
                }

                Map<String, String> rowData = new LinkedHashMap<>();

                for (String column : detectedColumns) {
                    rowData.put(column, getValue(record, column));
                }

                previewRows.add(rowData);
                count++;
            }

            Map<String, String> suggestedMapping = suggestMapping(detectedColumns);

            return CsvPreviewResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .detectedColumns(detectedColumns)
                    .previewRows(previewRows)
                    .suggestedMapping(suggestedMapping)
                    .message("CSV preview generated successfully")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("CSV preview failed: " + e.getMessage());
        }
    }

    @Override
    public CsvImportResponse importContacts(MultipartFile file) {

        CsvImportMappingRequest defaultMapping = CsvImportMappingRequest.builder()
                .fullNameColumn("Full Name")
                .companyColumn("Company")
                .emailColumn("Email")
                .phoneColumn("Phone")
                .jobTitleColumn("Job Title")
                .industryColumn("Industry")
                .websiteColumn("Website")
                .addressColumn("Address")
                .countryColumn("Country")
                .stateColumn("State")
                .cityColumn("City")
                .tagsColumn("Tags")
                .notesColumn("Notes")
                .build();

        return importContactsWithMapping(file, defaultMapping);
    }

    @Override
    public CsvImportResponse importContactsWithMapping(
            MultipartFile file,
            CsvImportMappingRequest mappingRequest
    ) {

        validateCsvFile(file);
        validateRequiredMapping(mappingRequest);

        int totalRecords = 0;
        int successfulImports = 0;
        int failedImports = 0;
        int duplicateRecords = 0;
        List<CsvImportError> errors = new ArrayList<>();

        try {
            Reader reader = new InputStreamReader(file.getInputStream());

            CSVParser csvParser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : csvParser) {

                totalRecords++;

                String fullName = getValue(record, mappingRequest.getFullNameColumn());
                String company = getValue(record, mappingRequest.getCompanyColumn());
                String email = getValue(record, mappingRequest.getEmailColumn());
                String phone = getValue(record, mappingRequest.getPhoneColumn());
                String jobTitle = getValue(record, mappingRequest.getJobTitleColumn());
                String industry = getValue(record, mappingRequest.getIndustryColumn());
                String website = getValue(record, mappingRequest.getWebsiteColumn());
                String address = getValue(record, mappingRequest.getAddressColumn());
                String country = getValue(record, mappingRequest.getCountryColumn());
                String state = getValue(record, mappingRequest.getStateColumn());
                String city = getValue(record, mappingRequest.getCityColumn());
                String tags = getValue(record, mappingRequest.getTagsColumn());
                String notes = getValue(record, mappingRequest.getNotesColumn());

                if (fullName == null || fullName.isBlank()) {
                    failedImports++;

                    errors.add(
                            CsvImportError.builder()
                                    .rowNumber(totalRecords)
                                    .reason("Full Name is required")
                                    .build()
                    );

                    continue;
                }

                if (email == null || email.isBlank()) {
                    failedImports++;

                    errors.add(
                            CsvImportError.builder()
                                    .rowNumber(totalRecords)
                                    .reason("Email is required")
                                    .build()
                    );

                    continue;
                }

                if (contactRepository.existsByEmail(email)) {
                    duplicateRecords++;

                    errors.add(
                            CsvImportError.builder()
                                    .rowNumber(totalRecords)
                                    .reason("Duplicate email: " + email)
                                    .build()
                    );

                    continue;
                }

                Contact contact = Contact.builder()
                        .fullName(fullName)
                        .company(company)
                        .email(email)
                        .phone(phone)
                        .jobTitle(jobTitle)
                        .industry(industry)
                        .website(website)
                        .address(address)
                        .country(country)
                        .state(state)
                        .city(city)
                        .tags(tags)
                        .notes(notes)
                        .status(ContactStatus.NEW)
                        .owner(UserContextHelper.currentUsername())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                contactRepository.save(contact);
                successfulImports++;
            }
            CsvImportHistory history = CsvImportHistory.builder()
                    .fileName(file.getOriginalFilename())
                    .totalRecords(totalRecords)
                    .successfulImports(successfulImports)
                    .failedImports(failedImports)
                    .duplicateRecords(duplicateRecords)
                    .importedAt(LocalDateTime.now())
                    .build();

            CsvImportHistory savedHistory = csvImportHistoryRepository.save(history);
            auditLogService.createAudit(
                    AuditAction.CSV_IMPORTED,
                    AuditModule.CSV_IMPORT,
                    "CSV Import",
                    savedHistory.getId(),
                    null,
                    file.getOriginalFilename(),
                    "Imported "
                            + successfulImports
                            + " contacts from "
                            + file.getOriginalFilename(),
                    "SYSTEM",
                    null
            );

            return CsvImportResponse.builder()
                    .importId(savedHistory.getId())
                    .fileName(savedHistory.getFileName())
                    .totalRecords(savedHistory.getTotalRecords())
                    .successfulImports(savedHistory.getSuccessfulImports())
                    .failedImports(savedHistory.getFailedImports())
                    .duplicateRecords(savedHistory.getDuplicateRecords())
                    .errors(errors)
                    .message("CSV import completed")
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("CSV import failed: " + e.getMessage());
        }
    }

    @Override
    public List<CsvImportHistory> getImportHistory() {
        return csvImportHistoryRepository.findAll();
    }

    @Override
    public CsvImportHistory getImportHistoryById(Long id) {
        return csvImportHistoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CSV import history not found"));
    }

    private void validateCsvFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new RuntimeException("Please upload a CSV file.");
        }

        String fileName = file.getOriginalFilename();

        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new RuntimeException("Only CSV files are allowed.");
        }

        long maxSize = 5 * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new RuntimeException("File size cannot exceed 5 MB.");
        }
    }

    private void validateRequiredMapping(CsvImportMappingRequest mappingRequest) {

        if (mappingRequest.getFullNameColumn() == null ||
                mappingRequest.getFullNameColumn().isBlank()) {
            throw new RuntimeException("Full name column mapping is required.");
        }

        if (mappingRequest.getEmailColumn() == null ||
                mappingRequest.getEmailColumn().isBlank()) {
            throw new RuntimeException("Email column mapping is required.");
        }
    }

    private String getValue(CSVRecord record, String columnName) {

        if (columnName == null || columnName.isBlank()) {
            return null;
        }

        try {
            if (!record.isMapped(columnName)) {
                return null;
            }

            return record.get(columnName);

        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> suggestMapping(List<String> detectedColumns) {

        Map<String, String> suggestedMapping = new LinkedHashMap<>();

        for (String column : detectedColumns) {

            String normalized = column.toLowerCase().replace(" ", "");

            if (normalized.equals("fullname") ||
                    normalized.equals("name") ||
                    normalized.equals("contactname")) {
                suggestedMapping.put("fullName", column);
            }

            if (normalized.equals("company") ||
                    normalized.equals("companyname") ||
                    normalized.equals("organization")) {
                suggestedMapping.put("company", column);
            }

            if (normalized.equals("email") ||
                    normalized.equals("emailaddress") ||
                    normalized.equals("mail")) {
                suggestedMapping.put("email", column);
            }

            if (normalized.equals("phone") ||
                    normalized.equals("phonenumber") ||
                    normalized.equals("mobile") ||
                    normalized.equals("mobilenumber")) {
                suggestedMapping.put("phone", column);
            }

            if (normalized.equals("jobtitle") ||
                    normalized.equals("designation") ||
                    normalized.equals("title")) {
                suggestedMapping.put("jobTitle", column);
            }

            if (normalized.equals("industry")) {
                suggestedMapping.put("industry", column);
            }

            if (normalized.equals("website") ||
                    normalized.equals("url")) {
                suggestedMapping.put("website", column);
            }

            if (normalized.equals("address")) {
                suggestedMapping.put("address", column);
            }

            if (normalized.equals("country")) {
                suggestedMapping.put("country", column);
            }

            if (normalized.equals("state")) {
                suggestedMapping.put("state", column);
            }

            if (normalized.equals("city")) {
                suggestedMapping.put("city", column);
            }

            if (normalized.equals("tags") ||
                    normalized.equals("tag")) {
                suggestedMapping.put("tags", column);
            }

            if (normalized.equals("notes") ||
                    normalized.equals("note") ||
                    normalized.equals("remarks")) {
                suggestedMapping.put("notes", column);
            }
        }

        return suggestedMapping;
    }
}