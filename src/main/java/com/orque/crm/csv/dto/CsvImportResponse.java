package com.orque.crm.csv.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportResponse {

    private Long importId;
    private String fileName;
    private Integer totalRecords;
    private Integer successfulImports;
    private Integer failedImports;
    private Integer duplicateRecords;
    private List<CsvImportError> errors;
    private String message;
}