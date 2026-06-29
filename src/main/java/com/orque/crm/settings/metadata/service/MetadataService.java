package com.orque.crm.settings.metadata.service;

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

    public List<CrmMetadataField> getFields(String moduleName) {
        return fieldRepository.findByModuleNameIgnoreCase(moduleName.toLowerCase());
    }

    @Transactional
    public CrmMetadataField createField(String moduleName, String name, String label, String type, 
                                        Boolean required, Boolean readonly, String options, String formula, String lookupModule) {
        CrmMetadataField field = CrmMetadataField.builder()
                .moduleName(moduleName.toLowerCase())
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
