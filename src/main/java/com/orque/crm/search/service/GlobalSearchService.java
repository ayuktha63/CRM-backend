package com.orque.crm.search.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GlobalSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, List<Map<String, Object>>> searchAll(String queryText) {
        Map<String, List<Map<String, Object>>> results = new LinkedHashMap<>();
        if (queryText == null || queryText.trim().length() < 2) {
            return results;
        }

        String searchPattern = "%" + queryText.trim().toLowerCase() + "%";

        // Query entities dynamically
        results.put("leads", searchEntity("Lead", Arrays.asList("fullName", "email", "company"), "fullName", searchPattern));
        results.put("contacts", searchEntity("Contact", Arrays.asList("fullName", "email", "company"), "fullName", searchPattern));
        results.put("accounts", searchEntity("Account", Arrays.asList("companyName", "industry"), "companyName", searchPattern));
        results.put("deals", searchEntity("Deal", Arrays.asList("dealName", "account"), "dealName", searchPattern));
        results.put("crm_tasks", searchEntity("CrmTask", Arrays.asList("title", "taskType"), "title", searchPattern));
        results.put("activities", searchEntity("Activity", Arrays.asList("subject", "type"), "subject", searchPattern));
        results.put("products", searchEntity("Product", Arrays.asList("name", "sku"), "name", searchPattern));
        results.put("quotes", searchEntity("Quote", Arrays.asList("quoteNumber", "contact"), "quoteNumber", searchPattern));
        results.put("invoices", searchEntity("Invoice", Arrays.asList("invoiceNumber", "contact"), "invoiceNumber", searchPattern));
        results.put("campaigns", searchEntity("Campaign", Arrays.asList("name", "subject"), "name", searchPattern));

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchEntity(String entityName, List<String> fields, String titleField, String pattern) {
        StringBuilder jpql = new StringBuilder("SELECT e.id, e." + titleField + " FROM " + entityName + " e WHERE ");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                jpql.append(" OR ");
            }
            jpql.append("LOWER(CAST(e.").append(fields.get(i)).append(" AS string)) LIKE :pattern");
        }
        
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Query query = entityManager.createQuery(jpql.toString());
            query.setParameter("pattern", pattern);
            query.setMaxResults(10);
            
            List<Object[]> rows = query.getResultList();
            for (Object[] row : rows) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", row[0]);
                map.put("title", row[1]);
                list.add(map);
            }
        } catch (Exception e) {
            // Ignore entities that are not fully configured or initialized
        }
        return list;
    }
}
