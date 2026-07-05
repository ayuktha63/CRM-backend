package com.orque.crm.feature.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Invoice;
import com.orque.crm.feature.entity.LineItem;
import com.orque.crm.feature.repository.InvoiceRepository;
import com.orque.crm.feature.repository.QuoteRepository;
import com.orque.crm.organization.entity.Organization;
import com.orque.crm.organization.repository.OrganizationRepository;
import com.orque.crm.pdf.PdfGeneratorService;
import com.orque.crm.settings.service.UserSettingsService;
import com.orque.crm.tax.dto.TaxBreakdown;
import com.orque.crm.tax.entity.OrganizationTaxSettings;
import com.orque.crm.tax.service.OrganizationTaxSettingsService;
import com.orque.crm.tax.service.TaxCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@CrossOrigin
public class InvoiceController {

    private static final String INVOICE_NOT_FOUND = "Invoice not found";
    private static final String STATUS_DRAFT      = "Draft";

    private final InvoiceRepository invoiceRepository;
    private final QuoteRepository quoteRepository;
    private final OrganizationRepository organizationRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final UserSettingsService userSettingsService;
    private final OrganizationTaxSettingsService taxSettingsService;
    private final TaxCalculationService taxCalculationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<List<Invoice>> getAll() {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        if (orgId == null) {
            return ResponseEntity.ok(invoiceRepository.findAll());
        }
        if (owner == null) {
            return ResponseEntity.ok(invoiceRepository.findByOrganizationId(orgId));
        }
        return ResponseEntity.ok(invoiceRepository.findByOrganizationIdAndCreatedBy(orgId, owner));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getById(@PathVariable Long id) {
        return invoiceRepository.findById(id)
                .map(i -> {
                    UserContextHelper.assertAccess(i.getCreatedBy());
                    return ResponseEntity.ok(i);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Invoice> save(@RequestBody Invoice invoice) {
        String currentUsername = UserContextHelper.currentUsername();
        if (invoice.getId() != null) {
            Invoice existing = invoiceRepository.findById(invoice.getId())
                    .orElseThrow(() -> new NoSuchElementException(INVOICE_NOT_FOUND));
            UserContextHelper.assertAccess(existing.getCreatedBy());
            applyTaxAndAmount(invoice, existing.getOrganizationId());
            existing.setInvoiceNumber(invoice.getInvoiceNumber());
            existing.setContact(invoice.getContact());
            existing.setAccount(invoice.getAccount());
            existing.setAmount(invoice.getAmount());
            existing.setDueDate(invoice.getDueDate());
            existing.setPaidDate(invoice.getPaidDate());
            existing.setStatus(invoice.getStatus());
            existing.setLineItems(invoice.getLineItems());
            existing.setCustomerState(invoice.getCustomerState());
            existing.setTaxSystem(invoice.getTaxSystem());
            existing.setTaxBreakdownJson(invoice.getTaxBreakdownJson());
            existing.setTotalTax(invoice.getTotalTax());
            existing.setGrandTotal(invoice.getGrandTotal());
            // Preserve original owner — edit does not reassign
            return ResponseEntity.ok(invoiceRepository.save(existing));
        }
        if (invoice.getStatus() == null) invoice.setStatus(STATUS_DRAFT);
        invoice.setCreatedBy(currentUsername);
        invoice.setOrganizationId(UserContextHelper.currentOrganizationId());
        applyTaxAndAmount(invoice, invoice.getOrganizationId());
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            invoice.setInvoiceNumber(userSettingsService.getAndIncrementInvoiceNumber());
        }
        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> update(@PathVariable Long id, @RequestBody Invoice invoice) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(INVOICE_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getCreatedBy());
        applyTaxAndAmount(invoice, existing.getOrganizationId());
        existing.setInvoiceNumber(invoice.getInvoiceNumber());
        existing.setContact(invoice.getContact());
        existing.setAccount(invoice.getAccount());
        existing.setAmount(invoice.getAmount());
        existing.setDueDate(invoice.getDueDate());
        existing.setPaidDate(invoice.getPaidDate());
        existing.setStatus(invoice.getStatus());
        existing.setLineItems(invoice.getLineItems());
        existing.setCustomerState(invoice.getCustomerState());
        existing.setTaxSystem(invoice.getTaxSystem());
        existing.setTaxBreakdownJson(invoice.getTaxBreakdownJson());
        existing.setTotalTax(invoice.getTotalTax());
        existing.setGrandTotal(invoice.getGrandTotal());
        return ResponseEntity.ok(invoiceRepository.save(existing));
    }

    /** Mirrors QuoteController's applyTaxAndAmount — see there for the full rationale. */
    private void applyTaxAndAmount(Invoice invoice, String organizationId) {
        List<LineItem> items = invoice.getLineItems();
        if (items == null || items.isEmpty()) return;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineItem item : items) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            item.setLineTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }
        invoice.setAmount(subtotal);

        OrganizationTaxSettings settings = taxSettingsService.findForOrg(organizationId);
        TaxBreakdown breakdown = taxCalculationService.calculate(settings, invoice.getCustomerState(), subtotal);
        invoice.setTaxSystem(breakdown.getTaxSystem());
        invoice.setTotalTax(breakdown.getTotalTax());
        invoice.setGrandTotal(breakdown.getGrandTotal());
        try {
            invoice.setTaxBreakdownJson(objectMapper.writeValueAsString(breakdown.getTaxes()));
        } catch (Exception e) {
            invoice.setTaxBreakdownJson(null);
        }
    }

    @PostMapping("/submit/{id}")
    public ResponseEntity<Invoice> submit(@PathVariable Long id) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(INVOICE_NOT_FOUND));
        existing.setStatus("Sent");
        return ResponseEntity.ok(invoiceRepository.save(existing));
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Invoice> approve(@PathVariable Long id) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(INVOICE_NOT_FOUND));
        existing.setStatus("Paid");
        existing.setPaidDate(LocalDate.now(ZoneId.systemDefault()));
        return ResponseEntity.ok(invoiceRepository.save(existing));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Invoice inv = invoiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(INVOICE_NOT_FOUND));
        UserContextHelper.assertAccess(inv.getCreatedBy());

        String quoteRef = inv.getQuoteId() != null
                ? quoteRepository.findById(inv.getQuoteId())
                        .map(q -> q.getQuoteNumber()).orElse("—")
                : "—";

        String statusClass = "Paid".equalsIgnoreCase(inv.getStatus()) ? "paid" : "";
        Organization org = inv.getOrganizationId() != null
                ? organizationRepository.findById(inv.getOrganizationId()).orElse(null)
                : null;

        Map<String, String> tokens = new HashMap<>(pdfGeneratorService.companyTokens(org));
        tokens.put("invoiceNumber", inv.getInvoiceNumber());
        tokens.put("date",          pdfGeneratorService.formatDateTime(inv.getCreatedAt()));
        tokens.put("dueDate",       pdfGeneratorService.formatDate(inv.getDueDate()));
        tokens.put("status",        inv.getStatus() != null ? inv.getStatus() : STATUS_DRAFT);
        tokens.put("statusClass",   statusClass);
        tokens.put("account",       inv.getAccount()   != null ? inv.getAccount()   : "—");
        tokens.put("contact",       inv.getContact()   != null ? inv.getContact()   : "—");
        tokens.put("createdBy",     inv.getCreatedBy() != null ? inv.getCreatedBy() : "—");
        tokens.put("quoteRef",      quoteRef);
        tokens.put("amount",        pdfGeneratorService.formatAmount(inv.getAmount()));
        tokens.put("taxRows",       pdfGeneratorService.buildTaxRows(inv.getTaxBreakdownJson(), inv.getAmount()));
        tokens.put("grandTotal",    pdfGeneratorService.formatAmount(
                inv.getGrandTotal() != null ? inv.getGrandTotal() : pdfGeneratorService.fallbackGrandTotal(inv.getAmount())));
        tokens.put("lineItemsRows", pdfGeneratorService.buildLineItemRows(inv.getLineItems(), inv.getAmount()));
        tokens.put("generatedAt",   pdfGeneratorService.nowFormatted());

        byte[] pdf = pdfGeneratorService.generate("invoice-template.html", tokens);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + inv.getInvoiceNumber() + ".pdf\"")
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(INVOICE_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getCreatedBy());
        invoiceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Invoice deleted successfully"));
    }
}
