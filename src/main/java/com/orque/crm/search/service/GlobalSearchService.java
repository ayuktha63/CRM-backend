package com.orque.crm.search.service;

import com.orque.crm.common.UserContextHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GlobalSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Owner field per entity — used to scope results for non-admin users.
     * Products and Campaigns have no owner concept; they are visible to all authenticated users.
     */
    private static final Map<String, String> ENTITY_OWNER_FIELD = Map.of(
        "Lead",     "assignedOwner",
        "Contact",  "owner",
        "Account",  "owner",
        "Deal",     "assignedTo",
        "CrmTask",  "assignedTo",
        "Activity", "assignedTo",
        "Quote",    "createdBy",
        "Invoice",  "createdBy"
    );

    public Map<String, List<Map<String, Object>>> searchAll(String queryText) {
        Map<String, List<Map<String, Object>>> results = new LinkedHashMap<>();
        if (queryText == null || queryText.trim().length() < 2) {
            return results;
        }

        boolean isAdmin = UserContextHelper.isAdmin();
        String currentUser = UserContextHelper.currentUsername();
        String searchPattern = "%" + queryText.trim().toLowerCase() + "%";

        results.put("leads",      searchEntity("Lead",     List.of("fullName","email","company"),       "fullName",     searchPattern, queryText, isAdmin, currentUser));
        results.put("contacts",   searchEntity("Contact",  List.of("fullName","email","company"),       "fullName",     searchPattern, queryText, isAdmin, currentUser));
        results.put("accounts",   searchEntity("Account",  List.of("companyName","industry"),           "companyName",  searchPattern, queryText, isAdmin, currentUser));
        results.put("deals",      searchEntity("Deal",     List.of("dealName","account"),               "dealName",     searchPattern, queryText, isAdmin, currentUser));
        results.put("crm_tasks",  searchEntity("CrmTask",  List.of("title","taskType"),                 "title",        searchPattern, queryText, isAdmin, currentUser));
        results.put("activities", searchEntity("Activity", List.of("subject","type"),                   "subject",      searchPattern, queryText, isAdmin, currentUser));
        results.put("products",   searchEntity("Product",  List.of("name","sku"),                       "name",         searchPattern, queryText, true, currentUser));  // no owner
        results.put("quotes",     searchEntity("Quote",    List.of("quoteNumber","contact"),            "quoteNumber",  searchPattern, queryText, isAdmin, currentUser));
        results.put("invoices",   searchEntity("Invoice",  List.of("invoiceNumber","contact"),          "invoiceNumber",searchPattern, queryText, isAdmin, currentUser));
        results.put("campaigns",  searchEntity("Campaign", List.of("name","subject"),                   "name",         searchPattern, queryText, true, currentUser));  // no owner

        results.put("notes",       searchNotes(searchPattern, queryText, isAdmin, currentUser));
        results.put("attachments", searchAttachments(searchPattern, queryText, isAdmin, currentUser));

        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchEntity(
            String entityName, List<String> fields, String titleField,
            String pattern, String rawQuery,
            boolean isAdmin, String currentUser) {

        String ownerField = ENTITY_OWNER_FIELD.get(entityName);

        StringBuilder jpql = new StringBuilder("SELECT e.id, e.").append(titleField)
                .append(" FROM ").append(entityName).append(" e WHERE (");
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) jpql.append(" OR ");
            jpql.append("LOWER(CAST(e.").append(fields.get(i)).append(" AS string)) LIKE :pattern");
        }
        jpql.append(")");

        if (!isAdmin && ownerField != null) {
            jpql.append(" AND e.").append(ownerField).append(" = :owner");
        }

        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Query query = entityManager.createQuery(jpql.toString());
            query.setParameter("pattern", pattern);
            if (!isAdmin && ownerField != null) {
                query.setParameter("owner", currentUser);
            }
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
            // Silently skip entities that are not yet initialized
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchNotes(String searchPattern, String queryText, boolean isAdmin, String currentUser) {
        List<Map<String, Object>> noteResults = new ArrayList<>();
        try {
            StringBuilder jpql = new StringBuilder(
                "SELECT n.id, n.content, n.moduleName, n.recordId FROM Note n WHERE LOWER(n.content) LIKE :pattern");
            if (!isAdmin) {
                jpql.append(" AND n.createdBy = :owner");
            }
            Query q = entityManager.createQuery(jpql.toString());
            q.setParameter("pattern", searchPattern);
            if (!isAdmin) q.setParameter("owner", currentUser);
            q.setMaxResults(10);

            for (Object[] row : (List<Object[]>) q.getResultList()) {
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
            // Ignore
        }
        return noteResults;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchAttachments(String searchPattern, String queryText, boolean isAdmin, String currentUser) {
        List<Map<String, Object>> attResults = new ArrayList<>();
        try {
            StringBuilder jpql = new StringBuilder(
                "SELECT a.id, a.fileName, a.moduleName, a.recordId FROM Attachment a WHERE LOWER(a.fileName) LIKE :pattern");
            if (!isAdmin) {
                jpql.append(" AND a.uploadedBy = :owner");
            }
            Query q = entityManager.createQuery(jpql.toString());
            q.setParameter("pattern", searchPattern);
            if (!isAdmin) q.setParameter("owner", currentUser);
            q.setMaxResults(10);

            for (Object[] row : (List<Object[]>) q.getResultList()) {
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
            // Ignore
        }
        return attResults;
    }

    private String getParentTitle(String moduleName, Long recordId) {
        if (moduleName == null || recordId == null) return "";
        try {
            String entityName;
            String titleField;
            switch (moduleName.toLowerCase()) {
                case "leads"              -> { entityName = "Lead";     titleField = "fullName"; }
                case "contacts"           -> { entityName = "Contact";  titleField = "fullName"; }
                case "accounts"           -> { entityName = "Account";  titleField = "companyName"; }
                case "deals"              -> { entityName = "Deal";     titleField = "dealName"; }
                case "tasks", "crm_tasks" -> { entityName = "CrmTask";  titleField = "title"; }
                case "activities"         -> { entityName = "Activity"; titleField = "subject"; }
                case "products"           -> { entityName = "Product";  titleField = "name"; }
                case "quotes"             -> { entityName = "Quote";    titleField = "quoteNumber"; }
                case "invoices"           -> { entityName = "Invoice";  titleField = "invoiceNumber"; }
                case "campaigns"          -> { entityName = "Campaign"; titleField = "name"; }
                default                   -> { return ""; }
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
