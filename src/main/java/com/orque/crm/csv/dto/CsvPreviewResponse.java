package com.orque.crm.csv.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvPreviewResponse {

    private String fileName;

    private List<String> detectedColumns;

    private List<Map<String, String>> previewRows;

    private Map<String, String> suggestedMapping;

    private String message;
}