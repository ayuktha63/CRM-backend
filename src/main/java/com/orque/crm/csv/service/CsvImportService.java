package com.orque.crm.csv.service;

import com.orque.crm.csv.dto.CsvImportMappingRequest;
import com.orque.crm.csv.dto.CsvImportResponse;
import com.orque.crm.csv.dto.CsvPreviewResponse;
import com.orque.crm.csv.entity.CsvImportHistory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CsvImportService {

    CsvPreviewResponse previewCsv(MultipartFile file);

    CsvImportResponse importContacts(MultipartFile file);

    CsvImportResponse importContactsWithMapping(
            MultipartFile file,
            CsvImportMappingRequest mappingRequest
    );

    List<CsvImportHistory> getImportHistory();

    CsvImportHistory getImportHistoryById(Long id);
}