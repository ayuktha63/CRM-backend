package com.orque.crm.report.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    public byte[] exportToExcel(List<Map<String, Object>> data)
            throws Exception {

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Report");

        if (data == null || data.isEmpty()) {

            workbook.close();

            return new byte[0];
        }

        int rowIndex = 0;

        Row header = sheet.createRow(rowIndex++);

        int column = 0;

        for (String key : data.get(0).keySet()) {

            header.createCell(column++)
                    .setCellValue(key);
        }

        for (Map<String, Object> rowData : data) {

            Row row = sheet.createRow(rowIndex++);

            column = 0;

            for (Object value : rowData.values()) {

                row.createCell(column++)
                        .setCellValue(
                                value == null
                                        ? ""
                                        : value.toString()
                        );
            }
        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        workbook.write(output);

        workbook.close();

        return output.toByteArray();
    }
}