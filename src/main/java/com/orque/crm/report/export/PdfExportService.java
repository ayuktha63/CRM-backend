package com.orque.crm.report.export;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    public byte[] exportToPdf(
            List<Map<String, Object>> data
    ) throws Exception {

        Document document = new Document();

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        PdfWriter.getInstance(
                document,
                output
        );

        document.open();

        document.add(
                new Paragraph("CRM Report")
        );

        document.add(
                new Paragraph(" ")
        );

        if (data != null && !data.isEmpty()) {

            PdfPTable table =
                    new PdfPTable(
                            data.get(0).size()
                    );

            for (String key : data.get(0).keySet()) {

                table.addCell(key);
            }

            for (Map<String, Object> row : data) {

                for (Object value : row.values()) {

                    table.addCell(
                            value == null
                                    ? ""
                                    : value.toString()
                    );
                }
            }

            document.add(table);
        }

        document.close();

        return output.toByteArray();
    }
}