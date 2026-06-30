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
        results.put("leads", searchEntity("Lead", Arrays.asList("fullName", "email", "company"), "fullName", searchPattern, queryText));
        results.put("contacts", searchEntity("Contact", Arrays.asList("fullName", "email", "company"), "fullName", searchPattern, queryText));
        results.put("accounts", searchEntity("Account", Arrays.asList("companyName", "industry"), "companyName", searchPattern, queryText));
        results.put("deals", searchEntity("Deal", Arrays.asList("dealName", "account"), "dealName", searchPattern, queryText));
        results.put("crm_tasks", searchEntity("CrmTask", Arrays.asList("title", "taskType"), "title", searchPattern, queryText));
        results.put("activities", searchEntity("Activity", Arrays.asList("subject", "type"), "subject", searchPattern, queryText));
        results.put("products", searchEntity("Product", Arrays.asList("name", "sku"), "name", searchPattern, queryText));
        results.put("quotes", searchEntity("Quote", Arrays.asList("quoteNumber", "contact"), "quoteNumber", searchPattern, queryText));
        results.put("invoices", searchEntity("Invoice", Arrays.asList("invoiceNumber", "contact"), "invoiceNumber", searchPattern, queryText));
        results.put("campaigns", searchEntity("Campaign", Arrays.asList("name", "subject"), "name", searchPattern, queryText));

        // 11. Search Notes (by content)
        List<Map<String, Object>> noteResults = new ArrayList<>();
        try {
            Query q = entityManager.createQuery("SELECT n.id, n.content, n.moduleName, n.recordId FROM Note n WHERE LOWER(n.content) LIKE :pattern");
            q.setParameter("pattern", searchPattern);
            q.setMaxResults(10);
            List<Object[]> rows = q.getResultList();
            for (Object[] row : rows) {
                Long id = (Long) row[0];
                String content = (String) row[1];
                String module = (String) row[2];
                Long parentId = (Long) row[3];
                
                String snippet = content.length() > 30 ? content.substring(0, 30) + "..." : content;
                String parentTitle = getParentTitle(module, parentId);
                String title = "Note: '" + snippet + "'" + (parentTitle.isEmpty() ? "" : " on " + module + " (" + parentTitle + ")");
                
                Map<String, Object> map = new HashMap<>();
                map.put("id", id);
                map.put("title", title);
                map.put("targetModule", module);
                map.put("targetId", parentId);
                noteResults.add(map);
            }
            rankResults(noteResults, queryText);
        } catch (Exception e) {
            // Ignore notes search failures
        }
        results.put("notes", noteResults);

        // 12. Search Attachments (by fileName)
        List<Map<String, Object>> attResults = new ArrayList<>();
        try {
            Query q = entityManager.createQuery("SELECT a.id, a.fileName, a.moduleName, a.recordId FROM Attachment a WHERE LOWER(a.fileName) LIKE :pattern");
            q.setParameter("pattern", searchPattern);
            q.setMaxResults(10);
            List<Object[]> rows = q.getResultList();
            for (Object[] row : rows) {
                Long id = (Long) row[0];
                String fileName = (String) row[1];
                String module = (String) row[2];
                Long parentId = (Long) row[3];
                
                String parentTitle = getParentTitle(module, parentId);
                String title = "File: " + fileName + (parentTitle.isEmpty() ? "" : " on " + module + " (" + parentTitle + ")");
                
                Map<String, Object> map = new HashMap<>();
                map.put("id", id);
                map.put("title", title);
                map.put("targetModule", module);
                map.put("targetId", parentId);
                attResults.add(map);
            }
            rankResults(attResults, queryText);
        } catch (Exception e) {
            // Ignore attachments search failures
        }
        results.put("attachments", attResults);

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchEntity(String entityName, List<String> fields, String titleField, String pattern, String rawQuery) {
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
            rankResults(list, rawQuery);
        } catch (Exception e) {
            // Ignore entities that are not fully configured or initialized
        }
        return list;
    }

    private String getParentTitle(String moduleName, Long recordId) {
        if (moduleName == null || recordId == null) return "";
        try {
            String entityName = "";
            String titleField = "";
            switch (moduleName.toLowerCase()) {
                case "leads" -> { entityName = "Lead"; titleField = "fullName"; }
                case "contacts" -> { entityName = "Contact"; titleField = "fullName"; }
                case "accounts" -> { entityName = "Account"; titleField = "companyName"; }
                case "deals" -> { entityName = "Deal"; titleField = "dealName"; }
                case "tasks", "crm_tasks" -> { entityName = "CrmTask"; titleField = "title"; }
                case "activities" -> { entityName = "Activity"; titleField = "subject"; }
                case "products" -> { entityName = "Product"; titleField = "name"; }
                case "quotes" -> { entityName = "Quote"; titleField = "quoteNumber"; }
                case "invoices" -> { entityName = "Invoice"; titleField = "invoiceNumber"; }
                case "campaigns" -> { entityName = "Campaign"; titleField = "name"; }
                default -> { return ""; }
            }
            Query q = entityManager.createQuery("SELECT e." + titleField + " FROM " + entityName + " e WHERE e.id = :id");
            q.setParameter("id", recordId);
            return (String) q.getSingleResult();
        } catch (Exception e) {
            return "";
        }
    }

    private void rankResults(List<Map<String, Object>> list, String queryText) {
        final String q = queryText.toLowerCase();
        list.sort((a, b) -> {
            String t1 = ((String) a.getOrDefault("title", "")).toLowerCase();
            String t2 = ((String) b.getOrDefault("title", "")).toLowerCase();
            int idx1 = t1.indexOf(q);
            int idx2 = t2.indexOf(q);
            if (idx1 != idx2) {
                if (idx1 == -1) return 1;
                if (idx2 == -1) return -1;
                return Integer.compare(idx1, idx2);
            }
            return t1.compareTo(t2);
        });
    }
}
