package com.orque.crm.feature.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Invoice;
import com.orque.crm.feature.entity.Quote;
import com.orque.crm.feature.repository.InvoiceRepository;
import com.orque.crm.feature.repository.QuoteRepository;
import com.orque.crm.pdf.PdfGeneratorService;
import com.orque.crm.settings.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
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
    private final PdfGeneratorService pdfGeneratorService;
    private final UserSettingsService userSettingsService;

    @GetMapping
    public ResponseEntity<List<Quote>> getAll() {
        return ResponseEntity.ok(quoteRepository.findAll().stream()
                .filter(q -> UserContextHelper.canAccess(q.getCreatedBy()))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Quote> getById(@PathVariable Long id) {
        return quoteRepository.findById(id)
                .map(q -> {
                    UserContextHelper.assertAccess(q.getCreatedBy());
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
            UserContextHelper.assertAccess(existing.getCreatedBy());
            existing.setQuoteNumber(quote.getQuoteNumber());
            existing.setContact(quote.getContact());
            existing.setAccount(quote.getAccount());
            existing.setAmount(quote.getAmount());
            existing.setValidUntil(quote.getValidUntil());
            existing.setStatus(quote.getStatus());
            // Preserve original owner — edit does not reassign
            return ResponseEntity.ok(quoteRepository.save(existing));
        }
        quote.setCreatedBy(currentUsername);
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
        UserContextHelper.assertAccess(existing.getCreatedBy());
        existing.setQuoteNumber(quote.getQuoteNumber());
        existing.setContact(quote.getContact());
        existing.setAccount(quote.getAccount());
        existing.setAmount(quote.getAmount());
        existing.setValidUntil(quote.getValidUntil());
        existing.setStatus(quote.getStatus());
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    @PostMapping("/submit/{id}")
    public ResponseEntity<Quote> submit(@PathVariable Long id) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        existing.setStatus("Sent");
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Quote> approve(@PathVariable Long id) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        existing.setStatus("Accepted");
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<Quote> reject(@PathVariable Long id) {
        Quote existing = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        existing.setStatus("Rejected");
        return ResponseEntity.ok(quoteRepository.save(existing));
    }

    @PostMapping("/{id}/invoice")
    public ResponseEntity<Invoice> generateInvoice(@PathVariable Long id) {
        Quote quote = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));

        if (!"Accepted".equalsIgnoreCase(quote.getStatus())) {
            throw new IllegalStateException("Invoice can only be generated from an Accepted quote");
        }

        String invoiceNumber = userSettingsService.getAndIncrementInvoiceNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .contact(quote.getContact())
                .account(quote.getAccount())
                .amount(quote.getAmount())
                .dueDate(LocalDate.now(ZoneId.systemDefault()).plusDays(30))
                .quoteId(id)
                .dealId(quote.getDealId())
                .createdBy(quote.getCreatedBy())
                .status(STATUS_DRAFT)
                .build();

        return ResponseEntity.ok(invoiceRepository.save(invoice));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Quote q = quoteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(QUOTE_NOT_FOUND));
        UserContextHelper.assertAccess(q.getCreatedBy());

        Map<String, String> tokens = Map.ofEntries(
                Map.entry("quoteNumber", q.getQuoteNumber()),
                Map.entry("date",        pdfGeneratorService.formatDateTime(q.getCreatedAt())),
                Map.entry("status",      q.getStatus() != null ? q.getStatus() : STATUS_DRAFT),
                Map.entry("account",     q.getAccount() != null ? q.getAccount() : "—"),
                Map.entry("contact",     q.getContact() != null ? q.getContact() : "—"),
                Map.entry("createdBy",   q.getCreatedBy() != null ? q.getCreatedBy() : "—"),
                Map.entry("amount",      pdfGeneratorService.formatAmount(q.getAmount())),
                Map.entry("tax",         pdfGeneratorService.calcTax(q.getAmount())),
                Map.entry("grandTotal",  pdfGeneratorService.calcGrandTotal(q.getAmount())),
                Map.entry("validUntil",  pdfGeneratorService.formatDate(q.getValidUntil())),
                Map.entry("generatedAt", pdfGeneratorService.nowFormatted())
        );

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
        UserContextHelper.assertAccess(existing.getCreatedBy());
        quoteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Quote deleted successfully"));
    }
}
