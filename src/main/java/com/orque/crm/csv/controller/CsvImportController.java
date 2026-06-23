package com.orque.crm.csv.controller;

import com.orque.crm.csv.dto.CsvImportMappingRequest;
import com.orque.crm.csv.dto.CsvImportResponse;
import com.orque.crm.csv.dto.CsvPreviewResponse;
import com.orque.crm.csv.entity.CsvImportHistory;
import com.orque.crm.csv.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/csv")
@RequiredArgsConstructor
public class CsvImportController {

    private final CsvImportService csvImportService;

    @PostMapping(
            value = "/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CsvPreviewResponse previewCsv(
            @RequestParam("file") MultipartFile file
    ) {
        return csvImportService.previewCsv(file);
    }

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CsvImportResponse importContacts(
            @RequestParam("file") MultipartFile file
    ) {
        return csvImportService.importContacts(file);
    }

    @PostMapping(
            value = "/import/mapped",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CsvImportResponse importContactsWithMapping(
            @RequestParam("file") MultipartFile file,
            @RequestParam String fullNameColumn,
            @RequestParam String emailColumn,
            @RequestParam(required = false) String phoneColumn,
            @RequestParam(required = false) String companyColumn,
            @RequestParam(required = false) String notesColumn
    ) {

        CsvImportMappingRequest mappingRequest =
                CsvImportMappingRequest.builder()
                        .fullNameColumn(fullNameColumn)
                        .emailColumn(emailColumn)
                        .phoneColumn(phoneColumn)
                        .companyColumn(companyColumn)
                        .notesColumn(notesColumn)
                        .build();

        return csvImportService.importContactsWithMapping(file, mappingRequest);
    }

    @GetMapping("/imports")
    public List<CsvImportHistory> getImportHistory() {
        return csvImportService.getImportHistory();
    }

    @GetMapping("/imports/{id}")
    public CsvImportHistory getImportHistoryById(
            @PathVariable Long id
    ) {
        return csvImportService.getImportHistoryById(id);
    }
}