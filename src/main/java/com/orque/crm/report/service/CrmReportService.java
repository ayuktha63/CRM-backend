package com.orque.crm.report.service;

import com.orque.crm.common.UserContextHelper;
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

    /**
     * Maps the frontend's lowercase-plural module name to the actual JPA entity name.
     * Naively capitalizing the module name (old behavior) produced "Contacts", "Leads",
     * "Deals", "Accounts" etc., none of which match the real (singular) @Entity classes,
     * so Hibernate rejected every module except "tasks" (the one case that had a manual
     * override).
     */
    private static final Map<String, String> MODULE_ENTITY_MAP = Map.of(
            "leads", "Lead",
            "contacts", "Contact",
            "accounts", "Account",
            "deals", "Deal",
            "tasks", "CrmTask",
            "campaigns", "Campaign",
            "activities", "Activity"
    );

    /** Restricts groupBy/column names to safe identifiers before they're concatenated into JPQL. */
    private static final java.util.regex.Pattern SAFE_IDENTIFIER = java.util.regex.Pattern.compile("^[a-zA-Z0-9_]+$");

    /** Aggregation expressions need a few extra characters (e.g. "COUNT(e.id) AS cnt"). */
    private static final java.util.regex.Pattern SAFE_AGGREGATION = java.util.regex.Pattern.compile("^[a-zA-Z0-9_,.()\\s*]+$");

    public List<CrmReport> getReports() {
        // Return reports shared publicly OR created by current user
        return repository.findByShareTypeIgnoreCaseOrCreatedByIgnoreCase(
                "PUBLIC", UserContextHelper.currentUsername());
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

        String moduleName = report.getModuleName() == null ? "" : report.getModuleName().toLowerCase().trim();
        String entityName = MODULE_ENTITY_MAP.get(moduleName);
        if (entityName == null) {
            return List.of(errorRow("Unsupported report module: " + report.getModuleName()));
        }

        String groupBy = report.getGroupBy() != null ? report.getGroupBy().trim() : null;
        if (groupBy != null && !groupBy.isEmpty() && !SAFE_IDENTIFIER.matcher(groupBy).matches()) {
            return List.of(errorRow("Invalid group-by field: " + groupBy));
        }

        List<String> columns = new ArrayList<>();
        if (report.getColumns() != null && !report.getColumns().isBlank()) {
            for (String col : report.getColumns().split(",")) {
                String trimmed = col.trim();
                if (!trimmed.isEmpty()) columns.add(trimmed);
            }
            for (String col : columns) {
                if (!SAFE_IDENTIFIER.matcher(col).matches()) {
                    return List.of(errorRow("Invalid column name: " + col));
                }
            }
        }

        String aggregations = report.getAggregations() != null ? report.getAggregations().trim() : null;
        if (aggregations != null && !aggregations.isEmpty() && !SAFE_AGGREGATION.matcher(aggregations).matches()) {
            return List.of(errorRow("Invalid aggregation expression"));
        }

        // Build dynamic query
        StringBuilder sql = new StringBuilder("SELECT ");
        List<String> columnsList = new ArrayList<>();

        if (groupBy != null && !groupBy.isEmpty()) {
            sql.append("e.").append(groupBy).append(", ");
            columnsList.add(groupBy);
        }

        if (aggregations != null && !aggregations.isEmpty()) {
            sql.append(aggregations);
            for (String agg : aggregations.split(",")) {
                columnsList.add(agg.trim());
            }
        } else if (!columns.isEmpty()) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("e.").append(columns.get(i));
                columnsList.add(columns.get(i));
            }
        } else {
            sql.append("e.id");
            columnsList.add("id");
        }

        sql.append(" FROM ").append(entityName).append(" e");

        // Tenant isolation — every module entity carries an organizationId; without this
        // filter the report engine could return every tenant's records to any user.
        String orgId = UserContextHelper.currentOrganizationId();
        if (orgId != null) {
            sql.append(" WHERE e.organizationId = :orgId");
        }

        if (groupBy != null && !groupBy.isEmpty()) {
            sql.append(" GROUP BY e.").append(groupBy);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try {
            Query query = entityManager.createQuery(sql.toString());
            if (orgId != null) {
                query.setParameter("orgId", orgId);
            }
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
            results.add(errorRow("Failed to query dynamic report database: " + e.getMessage()));
        }

        return results;
    }

    private Map<String, Object> errorRow(String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", message);
        return error;
    }
}
