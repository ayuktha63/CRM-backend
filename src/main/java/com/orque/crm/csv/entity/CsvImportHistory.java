package com.orque.crm.csv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "csv_import_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private Integer totalRecords;

    private Integer successfulImports;

    private Integer failedImports;

    private Integer duplicateRecords;

    private LocalDateTime importedAt;
}