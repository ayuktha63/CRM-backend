package com.orque.crm.report.service;

import com.orque.crm.report.entity.CrmReport;
import com.orque.crm.report.repository.CrmReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CrmReportService {

    private final CrmReportRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<CrmReport> getReports() {
        return repository.findAll();
    }

    @Transactional
    public CrmReport saveReport(CrmReport report, String username) {
        report.setCreatedBy(username);
        return repository.save(report);
    }

    @Transactional
    public void deleteReport(Long id) {
        repository.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> executeReport(Long reportId) {
        CrmReport report = repository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("Report not found"));

        // Build dynamic query
        StringBuilder sql = new StringBuilder("SELECT ");
        String primaryTable = report.getModuleName().toLowerCase();
        
        List<String> columnsList = new ArrayList<>();
        
        // Map columns or aggregates
        if (report.getGroupBy() != null && !report.getGroupBy().isBlank()) {
            sql.append("e.").append(report.getGroupBy()).append(", ");
            columnsList.add(report.getGroupBy());
        }

        // Dynamic aggregations mapping
        if (report.getAggregations() != null && !report.getAggregations().isBlank()) {
            sql.append(report.getAggregations());
            // Add aggregate headers
            String[] aggs = report.getAggregations().split(",");
            for (String agg : aggs) {
                columnsList.add(agg.trim());
            }
        } else if (report.getColumns() != null && !report.getColumns().isBlank()) {
            String[] cols = report.getColumns().split(",");
            for (int i = 0; i < cols.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append("e.").append(cols[i].trim());
                columnsList.add(cols[i].trim());
            }
        } else {
            sql.append("e.id");
            columnsList.add("id");
        }

        // JPA query needs mapping from Entity name (uppercase starting)
        String entityName = primaryTable.substring(0, 1).toUpperCase() + primaryTable.substring(1);
        if (entityName.equalsIgnoreCase("crm_tasks") || entityName.equalsIgnoreCase("tasks")) {
            entityName = "CrmTask";
        }
        sql.append(" FROM ").append(entityName).append(" e");

        // Group by
        if (report.getGroupBy() != null && !report.getGroupBy().isBlank()) {
            sql.append(" GROUP BY e.").append(report.getGroupBy());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Query query = entityManager.createQuery(sql.toString());
            query.setMaxResults(100);
            
            List<Object> rows = query.getResultList();
            for (Object row : rows) {
                Map<String, Object> map = new LinkedHashMap<>();
                if (row instanceof Object[]) {
                    Object[] rowArray = (Object[]) row;
                    for (int i = 0; i < Math.min(rowArray.length, columnsList.size()); i++) {
                        map.put(columnsList.get(i), rowArray[i]);
                    }
                } else {
                    map.put(columnsList.get(0), row);
                }
                results.add(map);
            }
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Failed to query dynamic report database: " + e.getMessage());
            results.add(error);
        }

        return results;
    }
}
