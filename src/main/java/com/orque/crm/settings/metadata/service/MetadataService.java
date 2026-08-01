package com.orque.crm.settings.metadata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.settings.metadata.entity.*;
import com.orque.crm.settings.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final CrmMetadataModuleRepository moduleRepository;
    private final CrmMetadataFieldRepository fieldRepository;
    private final CrmMetadataLayoutRepository layoutRepository;
    private final CrmCustomFieldDataRepository customFieldDataRepository;
    private final CrmCustomModuleRecordRepository customModuleRecordRepository;
    private final ObjectMapper objectMapper;

    public List<CrmMetadataModule> getModules() {
        return moduleRepository.findAll();
    }

    @Transactional
    public CrmMetadataModule createModule(String name, String label) {
        String moduleKey = name.trim().toLowerCase();
        Optional<CrmMetadataModule> existing = moduleRepository.findByNameIgnoreCase(moduleKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        CrmMetadataModule module = CrmMetadataModule.builder()
                .name(moduleKey)
                .label(label)
                .isCustom(true)
                .build();
        return moduleRepository.save(module);
    }

    /** Tenant-scoped: only this org's fields, plus any legacy rows saved before
     *  organizationId existed (null-org) so old data doesn't just vanish. */
    public List<CrmMetadataField> getFields(String moduleName) {
        String orgId = UserContextHelper.scopedOrgId();
        List<CrmMetadataField> all = fieldRepository.findByModuleNameIgnoreCase(moduleName.toLowerCase());
        if (orgId == null) return all;
        return all.stream()
                .filter(f -> f.getOrganizationId() == null || f.getOrganizationId().equals(orgId))
                .toList();
    }

    @Transactional
    public CrmMetadataField createField(String moduleName, String name, String label, String type,
                                        Boolean required, Boolean readonly, String options, String formula, String lookupModule) {
        CrmMetadataField field = CrmMetadataField.builder()
                .moduleName(moduleName.toLowerCase())
                .organizationId(UserContextHelper.scopedOrgId())
                .createdBy(UserContextHelper.currentUsername())
                .name(name.trim())
                .label(label)
                .type(type.toUpperCase())
                .isRequired(required != null && required)
                .isReadonly(readonly != null && readonly)
                .selectOptions(options)
                .formulaExpression(formula)
                .lookupTargetModule(lookupModule)
                .build();
        return fieldRepository.save(field);
    }

    /** Fields from another tenant are invisible to getFields() already, but a direct
     *  update/delete by id needs its own check — otherwise a guessed id from another
     *  org could be edited or removed cross-tenant. */
    private CrmMetadataField requireOwnedField(Long fieldId) {
        CrmMetadataField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Field not found."));
        String orgId = UserContextHelper.scopedOrgId();
        if (orgId != null && field.getOrganizationId() != null && !orgId.equals(field.getOrganizationId())) {
            throw new RuntimeException("Access denied.");
        }
        return field;
    }

    @Transactional
    public CrmMetadataField updateField(Long fieldId, String name, String label, String type,
                                        Boolean required, Boolean readonly, String options, String formula, String lookupModule) {
        CrmMetadataField field = requireOwnedField(fieldId);
        if (name != null && !name.trim().isEmpty() && !name.trim().equals(field.getName())) {
            String oldName = field.getName();
            String newName = name.trim();
            // Values already saved under the old field name (crm_custom_field_data) and any
            // saved Layout Builder order/visibility entry both key off the name, not the id —
            // rename them in place so a rename doesn't silently orphan existing data.
            customFieldDataRepository.findByModuleNameIgnoreCaseAndFieldNameIgnoreCase(field.getModuleName(), oldName)
                    .forEach(d -> { d.setFieldName(newName); customFieldDataRepository.save(d); });
            renameFieldInLayouts(field.getModuleName(), oldName, newName);
            field.setName(newName);
        }
        if (label != null) field.setLabel(label);
        if (type != null) field.setType(type.toUpperCase());
        if (required != null) field.setIsRequired(required);
        if (readonly != null) field.setIsReadonly(readonly);
        field.setSelectOptions(options);
        field.setFormulaExpression(formula);
        field.setLookupTargetModule(lookupModule);
        return fieldRepository.save(field);
    }

    @SuppressWarnings("unchecked")
    private void renameFieldInLayouts(String moduleName, String oldName, String newName) {
        for (CrmMetadataLayout layout : layoutRepository.findAllByModuleNameIgnoreCase(moduleName)) {
            try {
                List<Map<String, Object>> entries = objectMapper.readValue(layout.getLayoutDefinition(), List.class);
                boolean changed = false;
                for (Map<String, Object> entry : entries) {
                    if (oldName.equals(entry.get("name"))) {
                        entry.put("name", newName);
                        changed = true;
                    }
                }
                if (changed) {
                    layout.setLayoutDefinition(objectMapper.writeValueAsString(entries));
                    layoutRepository.save(layout);
                }
            } catch (Exception ignored) {
                // malformed saved layout — leave it alone rather than fail the rename
            }
        }
    }

    @Transactional
    public void deleteField(Long fieldId) {
        CrmMetadataField field = requireOwnedField(fieldId);
        customFieldDataRepository.deleteByModuleNameIgnoreCaseAndFieldNameIgnoreCase(field.getModuleName(), field.getName());
        fieldRepository.delete(field);
    }

    /** The Layout Builder's saved field order/visibility for a module, tenant-scoped.
     *  Returns an empty list if nothing's been saved yet — the frontend then falls back
     *  to the module's natural field order, all visible. */
    public List<Map<String, Object>> getLayout(String moduleName) {
        String moduleKey = moduleName.toLowerCase();
        String orgId = UserContextHelper.scopedOrgId();
        Optional<CrmMetadataLayout> layout = layoutRepository
                .findByModuleNameIgnoreCaseAndOrganizationIdAndRoleNameIsNull(moduleKey, orgId);
        if (layout.isEmpty()) return List.of();
        try {
            return objectMapper.readValue(layout.get().getLayoutDefinition(), List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Upserts the Layout Builder's field order/visibility for a module (one layout per
     *  tenant+module — role-specific layouts are a documented future extension, not used
     *  by the current builder UI, hence roleName is always null here). */
    @Transactional
    public List<Map<String, Object>> saveLayout(String moduleName, List<Map<String, Object>> fieldsOrder) {
        String moduleKey = moduleName.toLowerCase();
        String orgId = UserContextHelper.scopedOrgId();
        String json;
        try {
            json = objectMapper.writeValueAsString(fieldsOrder);
        } catch (Exception e) {
            throw new RuntimeException("Invalid layout payload.");
        }

        CrmMetadataLayout layout = layoutRepository
                .findByModuleNameIgnoreCaseAndOrganizationIdAndRoleNameIsNull(moduleKey, orgId)
                .orElseGet(() -> CrmMetadataLayout.builder()
                        .moduleName(moduleKey)
                        .organizationId(orgId)
                        .name("default")
                        .build());
        layout.setLayoutDefinition(json);
        layoutRepository.save(layout);
        return fieldsOrder;
    }

    /**
     * Bulk-loads custom-field values for a page of *system* module records (Leads,
     * Contacts, etc — anything with its own real entity/table) so they can be merged
     * in as extra columns on the module's list screen. One query for the whole page
     * instead of one round trip per row.
     */
    public Map<Long, Map<String, String>> getFieldValues(String moduleName, List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) return Map.of();
        String moduleKey = moduleName.toLowerCase();
        String orgId = UserContextHelper.scopedOrgId();
        List<CrmCustomFieldData> rows = customFieldDataRepository
                .findByModuleNameIgnoreCaseAndOrganizationIdAndRecordIdIn(moduleKey, orgId, recordIds);
        Map<Long, Map<String, String>> result = new HashMap<>();
        for (CrmCustomFieldData row : rows) {
            result.computeIfAbsent(row.getRecordId(), k -> new HashMap<>()).put(row.getFieldName(), row.getFieldValue());
        }
        return result;
    }

    /** Upserts custom-field values for a single *system* module record (a real Lead,
     *  Contact, etc — recordId is that entity's own id, not a CrmCustomModuleRecord). */
    @Transactional
    public Map<String, String> saveFieldValues(String moduleName, Long recordId, Map<String, Object> values) {
        String moduleKey = moduleName.toLowerCase();
        String orgId = UserContextHelper.scopedOrgId();

        customFieldDataRepository.deleteByModuleNameIgnoreCaseAndOrganizationIdAndRecordId(moduleKey, orgId, recordId);

        Map<String, String> saved = new LinkedHashMap<>();
        if (values == null) return saved;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() == null) continue;
            String value = String.valueOf(entry.getValue());
            customFieldDataRepository.save(CrmCustomFieldData.builder()
                    .moduleName(moduleKey)
                    .organizationId(orgId)
                    .recordId(recordId)
                    .fieldName(entry.getKey())
                    .fieldValue(value)
                    .build());
            saved.put(entry.getKey(), value);
        }
        return saved;
    }

    @Transactional
    public Map<String, Object> saveCustomRecord(String moduleName, Long recordId, Map<String, Object> values, String username) {
        String moduleKey = moduleName.toLowerCase();
        Long actualRecordId = recordId;

        // If creating a brand new record for a custom module
        if (actualRecordId == null || actualRecordId <= 0) {
            CrmCustomModuleRecord record = CrmCustomModuleRecord.builder()
                    .moduleName(moduleKey)
                    .createdBy(username != null ? username : "system")
                    .build();
            record = customModuleRecordRepository.save(record);
            actualRecordId = record.getId();
        }

        // Clean existing custom field data for this record to prevent duplicate mappings
        customFieldDataRepository.deleteByModuleNameIgnoreCaseAndRecordId(moduleKey, actualRecordId);

        // Fetch dynamic auto-number fields to increment
        List<CrmMetadataField> fields = getFields(moduleKey);
        for (CrmMetadataField field : fields) {
            if ("AUTO_NUMBER".equalsIgnoreCase(field.getType())) {
                // Generate sequential identifier
                String count = String.valueOf(customModuleRecordRepository.findByModuleNameIgnoreCase(moduleKey).size() + 1000);
                values.put(field.getName(), count);
            }
        }

        // Save fields
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() == null) continue;
            CrmCustomFieldData data = CrmCustomFieldData.builder()
                    .moduleName(moduleKey)
                    .organizationId(UserContextHelper.scopedOrgId())
                    .recordId(actualRecordId)
                    .fieldName(entry.getKey())
                    .fieldValue(String.valueOf(entry.getValue()))
                    .build();
            customFieldDataRepository.save(data);
        }

        // Return compiled record map
        return getCustomRecord(moduleKey, actualRecordId);
    }

    public Map<String, Object> getCustomRecord(String moduleName, Long recordId) {
        String moduleKey = moduleName.toLowerCase();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", recordId);

        List<CrmCustomFieldData> dataList = customFieldDataRepository.findByModuleNameIgnoreCaseAndRecordId(moduleKey, recordId);
        for (CrmCustomFieldData data : dataList) {
            record.put(data.getFieldName(), data.getFieldValue());
        }

        // Apply formula evaluation
        List<CrmMetadataField> fields = getFields(moduleKey);
        for (CrmMetadataField field : fields) {
            if ("FORMULA".equalsIgnoreCase(field.getType()) && field.getFormulaExpression() != null) {
                String val = evaluateFormula(field.getFormulaExpression(), record);
                record.put(field.getName(), val);
            }
        }

        return record;
    }

    public List<Map<String, Object>> getCustomRecords(String moduleName) {
        String moduleKey = moduleName.toLowerCase();
        List<CrmCustomModuleRecord> baseRecords = customModuleRecordRepository.findByModuleNameIgnoreCase(moduleKey);
        List<Map<String, Object>> results = new ArrayList<>();
        for (CrmCustomModuleRecord base : baseRecords) {
            results.add(getCustomRecord(moduleKey, base.getId()));
        }
        return results;
    }

    /** Simple regex formula evaluator. Supports string concat and basic math. */
    public String evaluateFormula(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) return "";

        String result = expression;
        // Find all {field_name} occurrences
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(expression);
        
        while (matcher.find()) {
            String field = matcher.group(1);
            String value = String.valueOf(context.getOrDefault(field, "0"));
            result = result.replace("{" + field + "}", value);
        }

        // Evaluate simple arithmetic if it looks numeric
        if (result.matches("[0-9\\s\\+\\-\\*\\/\\(\\)\\.]+")) {
            try {
                return String.valueOf(evaluateMathExpression(result));
            } catch (Exception e) {
                return result; // return string representation if math parsing fails
            }
        }

        return result;
    }

    private double evaluateMathExpression(String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }
}
