package com.orque.crm.csv.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportError {

    private int rowNumber;
    private String reason;
}