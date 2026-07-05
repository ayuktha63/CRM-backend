package com.orque.crm.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import com.orque.crm.feature.entity.LineItem;
import com.orque.crm.organization.entity.Organization;
import com.orque.crm.tax.dto.TaxComponent;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final BigDecimal GST_RATE        = new BigDecimal("0.18");
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    /** Legacy flat-18%-GST fallback, used only when a record predates the tax engine. */
    public String calcTax(BigDecimal amount) {
        if (amount == null) return "0.00";
        return formatAmount(amount.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP));
    }

    public BigDecimal fallbackGrandTotal(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.ONE.add(GST_RATE)).setScale(2, RoundingMode.HALF_UP);
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

    /**
     * Builds the "Description / Qty / Unit Price / Total" table rows for a Quote/Invoice's
     * product lines. Falls back to a single placeholder row for legacy records saved
     * before line items existed (flat `amount` only, no products attached).
     */
    public String buildLineItemRows(List<LineItem> items, BigDecimal fallbackAmount) {
        if (items == null || items.isEmpty()) {
            return "<tr bgcolor=\"#ffffff\">"
                    + "<td style=\"border-bottom: 1px solid #e5e7eb;\">—</td>"
                    + "<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">1</td>"
                    + "<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">" + formatAmount(fallbackAmount) + "</td>"
                    + "<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">" + formatAmount(fallbackAmount) + "</td>"
                    + "</tr>";
        }
        StringBuilder rows = new StringBuilder();
        for (LineItem item : items) {
            rows.append("<tr bgcolor=\"#ffffff\">")
                    .append("<td style=\"border-bottom: 1px solid #e5e7eb;\">").append(escapeHtml(item.getProductName())).append("</td>")
                    .append("<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">").append(item.getQuantity() != null ? item.getQuantity() : 0).append("</td>")
                    .append("<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">").append(formatAmount(item.getUnitPrice())).append("</td>")
                    .append("<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">").append(formatAmount(item.getLineTotal())).append("</td>")
                    .append("</tr>");
        }
        return rows.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Builds the tax-component rows (e.g. "CGST 9%" / "SGST 9%" for India, or a single
     * "VAT 5%" row for UAE/Kenya) from the JSON snapshot TaxCalculationService stored on
     * the Quote/Invoice at save time. Falls back to a flat 18% GST row for records saved
     * before the tax engine existed (taxBreakdownJson is null on those).
     */
    public String buildTaxRows(String taxBreakdownJson, BigDecimal fallbackAmount) {
        List<TaxComponent> components = null;
        if (taxBreakdownJson != null && !taxBreakdownJson.isBlank()) {
            try {
                components = objectMapper.readValue(taxBreakdownJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, TaxComponent.class));
            } catch (Exception e) {
                components = null;
            }
        }
        if (components == null || components.isEmpty()) {
            return "<tr><td style=\"border-bottom: 1px solid #e5e7eb;\"><font color=\"#374151\">GST (18%)</font></td>"
                    + "<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">Rs " + calcTax(fallbackAmount) + "</td></tr>";
        }
        StringBuilder rows = new StringBuilder();
        for (TaxComponent c : components) {
            rows.append("<tr><td style=\"border-bottom: 1px solid #e5e7eb;\"><font color=\"#374151\">")
                    .append(escapeHtml(c.getName())).append(" ").append(c.getRate()).append("%</font></td>")
                    .append("<td align=\"right\" style=\"border-bottom: 1px solid #e5e7eb;\">Rs ")
                    .append(formatAmount(c.getAmount())).append("</td></tr>");
        }
        return rows.toString();
    }
}
