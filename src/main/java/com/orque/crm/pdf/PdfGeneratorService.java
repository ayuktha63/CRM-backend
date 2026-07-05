package com.orque.crm.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import com.orque.crm.organization.entity.Organization;
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
import java.util.HashMap;
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

    /**
     * Tokens for the tenant's own company/billing details, shown as "BILLED FROM"/"FROM"
     * and "Payment Details" on Quote/Invoice PDFs. Falls back to sensible defaults so a
     * tenant that hasn't filled in its billing profile yet still gets a usable PDF.
     */
    public Map<String, String> companyTokens(Organization org) {
        Map<String, String> tokens = new HashMap<>();
        if (org == null) {
            tokens.put("companyName", "—");
            tokens.put("companyTagline", "");
            tokens.put("companyGstin", "—");
            tokens.put("companyEmail", "—");
            tokens.put("bankName", "—");
            tokens.put("bankAccountNumber", "—");
            tokens.put("bankIfsc", "—");
            tokens.put("upiId", "—");
            tokens.put("paymentTermsDays", "30");
            tokens.put("lateFeeText", "—");
            return tokens;
        }
        String companyName = org.getLegalName() != null && !org.getLegalName().isBlank()
                ? org.getLegalName() : org.getOrganizationName();
        tokens.put("companyName", companyName != null ? companyName : "—");
        tokens.put("companyTagline", org.getCompanyTagline() != null ? org.getCompanyTagline() : "");
        tokens.put("companyGstin", org.getGstin() != null ? org.getGstin() : "—");
        tokens.put("companyEmail", org.getEmail() != null ? org.getEmail() : "—");
        tokens.put("bankName", org.getBankName() != null ? org.getBankName() : "—");
        tokens.put("bankAccountNumber", org.getBankAccountNumber() != null ? org.getBankAccountNumber() : "—");
        tokens.put("bankIfsc", org.getBankIfsc() != null ? org.getBankIfsc() : "—");
        tokens.put("upiId", org.getUpiId() != null ? org.getUpiId() : "—");
        tokens.put("paymentTermsDays", org.getPaymentTermsDays() != null ? String.valueOf(org.getPaymentTermsDays()) : "30");
        tokens.put("lateFeeText", org.getLateFeeText() != null ? org.getLateFeeText() : "—");
        return tokens;
    }
}
