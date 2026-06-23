package com.orque.crm.csv.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportMappingRequest {

    private String fullNameColumn;

    private String companyColumn;

    private String emailColumn;

    private String phoneColumn;

    private String jobTitleColumn;

    private String industryColumn;

    private String websiteColumn;

    private String addressColumn;

    private String countryColumn;

    private String stateColumn;

    private String cityColumn;

    private String tagsColumn;

    private String notesColumn;
}