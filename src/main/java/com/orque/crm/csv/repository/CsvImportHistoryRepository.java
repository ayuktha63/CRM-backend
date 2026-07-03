package com.orque.crm.csv.repository;

import com.orque.crm.csv.entity.CsvImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvImportHistoryRepository extends JpaRepository<CsvImportHistory, Long> {

    java.util.List<CsvImportHistory> findByOrganizationIdOrderByImportedAtDesc(String organizationId);
}