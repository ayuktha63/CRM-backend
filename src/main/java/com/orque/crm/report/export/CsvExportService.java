package com.orque.crm.report.export;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CsvExportService {

    public String exportToCsv(List<Map<String, Object>> data) {

        if (data == null || data.isEmpty()) {
            return "No data available";
        }

        StringBuilder csv = new StringBuilder();

        Map<String, Object> firstRow = data.get(0);

        csv.append(String.join(",", firstRow.keySet()));
        csv.append("\n");

        for (Map<String, Object> row : data) {
            csv.append(
                    row.values()
                            .stream()
                            .map(value -> value == null ? "" : value.toString().replace(",", " "))
                            .reduce((a, b) -> a + "," + b)
                            .orElse("")
            );
            csv.append("\n");
        }

        return csv.toString();
    }
}