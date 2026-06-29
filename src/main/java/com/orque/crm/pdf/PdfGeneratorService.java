package com.orque.crm.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final BigDecimal GST_RATE        = new BigDecimal("0.18");

    /**
     * Load an HTML template from classpath (resources/templates/pdf/) and replace
     * all {{placeholder}} tokens with the provided values.
     */
    public byte[] generate(String templateName, Map<String, String> tokens) {
        try {
            String html = loadTemplate(templateName);
            for (Map.Entry<String, String> e : tokens.entrySet()) {
                html = html.replace("{{" + e.getKey() + "}}", e.getValue() == null ? "" : e.getValue());
            }
            return renderHtmlToPdf(html);
        } catch (IOException e) {
            throw new IllegalStateException("PDF generation failed for template: " + templateName, e);
        }
    }

    private String loadTemplate(String name) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/pdf/" + name);
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("deprecation")
    private byte[] renderHtmlToPdf(String html) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 54);
        PdfWriter.getInstance(document, out);
        document.open();
        HTMLWorker worker = new HTMLWorker(document);
        worker.parse(new StringReader(html));
        document.close();
        return out.toByteArray();
    }

    // ── Convenience helpers ──────────────────────────────────────────────────

    public String formatDate(LocalDate d) {
        return d == null ? "—" : d.format(DATE_FMT);
    }

    public String formatDateTime(LocalDateTime dt) {
        return dt == null ? "—" : dt.format(DT_FMT);
    }

    public String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }

    public String calcTax(BigDecimal amount) {
        if (amount == null) return "0.00";
        return formatAmount(amount.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP));
    }

    public String calcGrandTotal(BigDecimal amount) {
        if (amount == null) return "0.00";
        return formatAmount(amount.multiply(BigDecimal.ONE.add(GST_RATE)).setScale(2, RoundingMode.HALF_UP));
    }

    public String nowFormatted() {
        return LocalDateTime.now().format(DT_FMT);
    }
}
