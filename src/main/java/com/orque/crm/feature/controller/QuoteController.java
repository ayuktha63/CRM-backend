package com.orque.crm.feature.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Invoice;
import com.orque.crm.feature.entity.LineItem;
import com.orque.crm.feature.entity.Quote;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/quotes")
@RequiredArgsConstructor
@CrossOrigin
public class QuoteController {

    private static final String QUOTE_NOT_FOUND = "Quote not found";
    private static final String STATUS_DRAFT    = "Draft";

    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final UserSettingsService userSettingsService;
    private final OrganizationTaxSettingsService taxSettingsService;
    private final TaxCalculationService taxCalculationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<List<Quote>> getAll() {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        if (orgId == null) {
            return ResponseEntity.ok(quoteRepository.findAll());
        }
        if (owner == null) {
            return ResponseEntity.ok(quoteRepository.findByOrganizationId(orgId));
        }
        return ResponseEntity.ok(quoteRepository.findByOrganizationIdAndCreatedBy(orgId, owner));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> getById(@PathVariable Long id) {
        return quoteRepository.findById(id)
                .map(q -> {
                    UserContextHelper.assertAccess(q.getOrganizationId(), q.getCreatedBy());
                    return ResponseEntity.ok(q);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Quote> save(@RequestBody Quote quote) {
        String currentUsername = UserContextHelper.currentUsername();
        if (quote.getId() != null) {
            Quote existing = quoteRepository.findById(quote.getId())
                    .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
            UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getCreatedBy());
            applyTaxAndAmount(quote, existing.getOrganizationId());
            existing.setQuoteNumber(quote.getQuoteNumber());
            existing.setContact(quote.getContact());
            existing.setAccount(quote.getAccount());
            existing.setAmount(quote.getAmount());
            existing.setValidUntil(quote.getValidUntil());
            existing.setStatus(quote.getStatus());
            existing.setLineItems(quote.getLineItems());
            existing.setCustomerState(quote.getCustomerState());
            existing.setTaxSystem(quote.getTaxSystem());
            existing.setTaxBreakdownJson(quote.getTaxBreakdownJson());
            existing.setTotalTax(quote.getTotalTax());
            existing.setGrandTotal(quote.getGrandTotal());
            // Preserve original owner — edit does not reassign
            return ResponseEntity.ok(quoteRepository.save(existing));
        }
        quote.setCreatedBy(currentUsername);
        quote.setOrganizationId(UserContextHelper.currentOrganizationId());
        applyTaxAndAmount(quote, quote.getOrganizationId());
        if (quote.getStatus() == null) quote.setStatus(STATUS_DRAFT);
        if (quote.getQuoteNumber() == null || quote.getQuoteNumber().isBlank()) {
            quote.setQuoteNumber(userSettingsService.getAndIncrementQuoteNumber());
        }
        return ResponseEntity.ok(quoteRepository.save(quote));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Quote> update(@PathVariable Long id, @RequestBody Quote quote) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getCreatedBy());
        applyTaxAndAmount(quote, existing.getOrganizationId());
        existing.setQuoteNumber(quote.getQuoteNumber());
        existing.setContact(quote.getContact());
        existing.setAccount(quote.getAccount());
        existing.setAmount(quote.getAmount());
        existing.setValidUntil(quote.getValidUntil());
        existing.setStatus(quote.getStatus());
        existing.setLineItems(quote.getLineItems());
        existing.setCustomerState(quote.getCustomerState());
        existing.setTaxSystem(quote.getTaxSystem());
        existing.setTaxBreakdownJson(quote.getTaxBreakdownJson());
        existing.setTotalTax(quote.getTotalTax());
        existing.setGrandTotal(quote.getGrandTotal());
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    /**
     * When line items are present: recomputes `amount` as the server-side sum of each
     * line's quantity × unit price (never trusts a client-supplied flat amount once real
     * line items exist), then runs it through TaxCalculationService — the single source
     * of truth for tax — using this org's current tax settings, and snapshots the
     * resulting breakdown onto the quote. Quotes with no line items (legacy flat-amount
     * entry) are left untouched, including their tax fields.
     */
    private void applyTaxAndAmount(Quote quote, String organizationId) {
        List<LineItem> items = quote.getLineItems();
        if (items == null || items.isEmpty()) return;
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LineItem item : items) {
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            item.setLineTotal(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }
        quote.setAmount(subtotal);

        OrganizationTaxSettings settings = taxSettingsService.findForOrg(organizationId);
        TaxBreakdown breakdown = taxCalculationService.calculate(settings, quote.getCustomerState(), subtotal);
        quote.setTaxSystem(breakdown.getTaxSystem());
        quote.setTotalTax(breakdown.getTotalTax());
        quote.setGrandTotal(breakdown.getGrandTotal());
        try {
            quote.setTaxBreakdownJson(objectMapper.writeValueAsString(breakdown.getTaxes()));
        } catch (Exception e) {
            quote.setTaxBreakdownJson(null);
        }
    }

    @PostMapping("/submit/{id}")
    public ResponseEntity<Quote> submit(@PathVariable Long id) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getCreatedBy());
        existing.setStatus("Sent");
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Quote> approve(@PathVariable Long id) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getCreatedBy());
        existing.setStatus("Accepted");
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<Quote> reject(@PathVariable Long id) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getCreatedBy());
        existing.setStatus("Rejected");
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    @PostMapping("/{id}/invoice")
    public ResponseEntity<Invoice> generateInvoice(@PathVariable Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(quote.getOrganizationId(), quote.getCreatedBy());

        if (!"Accepted".equalsIgnoreCase(quote.getStatus())) {
            throw new IllegalStateException("Invoice can only be generated from an Accepted quote");
        }

        String invoiceNumber = userSettingsService.getAndIncrementInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .organizationId(quote.getOrganizationId())
                .contact(quote.getContact())
                .account(quote.getAccount())
                .amount(quote.getAmount())
                .dueDate(LocalDate.now(ZoneId.systemDefault()).plusDays(30))
                .quoteId(id)
                .dealId(quote.getDealId())
                .createdBy(quote.getCreatedBy())
                .status(STATUS_DRAFT)
                .lineItems(new ArrayList<>(quote.getLineItems()))
                .customerState(quote.getCustomerState())
                .taxSystem(quote.getTaxSystem())
                .taxBreakdownJson(quote.getTaxBreakdownJson())
                .totalTax(quote.getTotalTax())
                .grandTotal(quote.getGrandTotal())
                .build();

        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Quote q = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(q.getOrganizationId(), q.getCreatedBy());

        Organization org = q.getOrganizationId() != null
                ? organizationRepository.findById(q.getOrganizationId()).orElse(null)
                : null;

        // Tax is recalculated fresh from the org's *current* Tax Settings at download
        // time, rather than trusting whatever taxSystem/taxBreakdownJson was frozen onto
        // this quote when it was created — so if the org later changes GST<->VAT or
        // updates a rate, every PDF immediately reflects the current configuration
        // instead of staying stuck with what was true at creation time.
        OrganizationTaxSettings settings = taxSettingsService.findForOrg(q.getOrganizationId());
        TaxBreakdown breakdown = taxCalculationService.calculate(settings, q.getCustomerState(), q.getAmount());
        String freshTaxBreakdownJson;
        try {
            freshTaxBreakdownJson = objectMapper.writeValueAsString(breakdown.getTaxes());
        } catch (Exception e) {
            freshTaxBreakdownJson = null;
        }

        Map<String, String> tokens = new HashMap<>(pdfGeneratorService.companyTokens(org));
        tokens.put("quoteNumber", q.getQuoteNumber());
        tokens.put("date",        pdfGeneratorService.formatDateTime(q.getCreatedAt()));
        tokens.put("status",      q.getStatus() != null ? q.getStatus() : STATUS_DRAFT);
        tokens.put("account",     q.getAccount() != null ? q.getAccount() : "—");
        tokens.put("contact",     q.getContact() != null ? q.getContact() : "—");
        tokens.put("createdBy",   q.getCreatedBy() != null ? q.getCreatedBy() : "—");
        tokens.put("amount",      pdfGeneratorService.formatAmount(q.getAmount()));
        tokens.put("taxRows",     pdfGeneratorService.buildTaxRows(freshTaxBreakdownJson, q.getAmount()));
        tokens.put("grandTotal",  pdfGeneratorService.formatAmount(breakdown.getGrandTotal()));
        tokens.put("lineItemsRows", pdfGeneratorService.buildLineItemRows(q.getLineItems(), q.getAmount()));
        tokens.put("validUntil",  pdfGeneratorService.formatDate(q.getValidUntil()));
        tokens.put("generatedAt", pdfGeneratorService.nowFormatted());

        byte[] pdf = pdfGeneratorService.generate("quote-template.html", tokens);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + q.getQuoteNumber() + ".pdf\"")
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getCreatedBy());
        quoteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Quote deleted successfully"));
    }
}
