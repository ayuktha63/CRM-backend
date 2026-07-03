package com.orque.crm.feature.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Invoice;
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
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@CrossOrigin
public class InvoiceController {

    private static final String INVOICE_NOT_FOUND = "Invoice not found";
    private static final String STATUS_DRAFT      = "Draft";

    private final InvoiceRepository invoiceRepository;
    private final QuoteRepository quoteRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final UserSettingsService userSettingsService;

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
            existing.setInvoiceNumber(invoice.getInvoiceNumber());
            existing.setContact(invoice.getContact());
            existing.setAccount(invoice.getAccount());
            existing.setAmount(invoice.getAmount());
            existing.setDueDate(invoice.getDueDate());
            existing.setPaidDate(invoice.getPaidDate());
            existing.setStatus(invoice.getStatus());
            // Preserve original owner — edit does not reassign
            return ResponseEntity.ok(invoiceRepository.save(existing));
        }
        if (invoice.getStatus() == null) invoice.setStatus(STATUS_DRAFT);
        invoice.setCreatedBy(currentUsername);
        invoice.setOrganizationId(UserContextHelper.currentOrganizationId());
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
        existing.setInvoiceNumber(invoice.getInvoiceNumber());
        existing.setContact(invoice.getContact());
        existing.setAccount(invoice.getAccount());
        existing.setAmount(invoice.getAmount());
        existing.setDueDate(invoice.getDueDate());
        existing.setPaidDate(invoice.getPaidDate());
        existing.setStatus(invoice.getStatus());
        return ResponseEntity.ok(invoiceRepository.save(existing));
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

        Map<String, String> tokens = Map.ofEntries(
                Map.entry("invoiceNumber", inv.getInvoiceNumber()),
                Map.entry("date",          pdfGeneratorService.formatDateTime(inv.getCreatedAt())),
                Map.entry("dueDate",       pdfGeneratorService.formatDate(inv.getDueDate())),
                Map.entry("status",        inv.getStatus() != null ? inv.getStatus() : STATUS_DRAFT),
                Map.entry("statusClass",   statusClass),
                Map.entry("account",       inv.getAccount()   != null ? inv.getAccount()   : "—"),
                Map.entry("contact",       inv.getContact()   != null ? inv.getContact()   : "—"),
                Map.entry("createdBy",     inv.getCreatedBy() != null ? inv.getCreatedBy() : "—"),
                Map.entry("quoteRef",      quoteRef),
                Map.entry("amount",        pdfGeneratorService.formatAmount(inv.getAmount())),
                Map.entry("tax",           pdfGeneratorService.calcTax(inv.getAmount())),
                Map.entry("grandTotal",    pdfGeneratorService.calcGrandTotal(inv.getAmount())),
                Map.entry("generatedAt",   pdfGeneratorService.nowFormatted())
        );

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
