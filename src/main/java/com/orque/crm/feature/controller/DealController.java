package com.orque.crm.feature.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.feature.entity.Activity;
import com.orque.crm.feature.entity.Deal;
import com.orque.crm.feature.entity.Invoice;
import com.orque.crm.feature.entity.Quote;
import com.orque.crm.feature.repository.ActivityRepository;
import com.orque.crm.feature.repository.DealRepository;
import com.orque.crm.feature.repository.InvoiceRepository;
import com.orque.crm.feature.repository.QuoteRepository;
import com.orque.crm.task.entity.CrmTask;
import com.orque.crm.task.repository.CrmTaskRepository;
import com.orque.crm.settings.service.UserSettingsService;
import com.orque.crm.tax.dto.TaxBreakdown;
import com.orque.crm.tax.entity.OrganizationTaxSettings;
import com.orque.crm.tax.service.OrganizationTaxSettingsService;
import com.orque.crm.tax.service.TaxCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
@CrossOrigin
public class DealController {

    private static final String DEAL_NOT_FOUND = "Deal not found";

    private final DealRepository dealRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final ActivityRepository activityRepository;
    private final CrmTaskRepository taskRepository;
    private final UserSettingsService userSettingsService;
    private final OrganizationTaxSettingsService taxSettingsService;
    private final TaxCalculationService taxCalculationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param assignedTo Optional — lets an admin filter to a specific salesperson's deals
     *                   (e.g. the Dashboard's "View as" picker). Ignored for non-admins,
     *                   who are always self-scoped via {@link UserContextHelper#scopedOwner()}
     *                   regardless of what's passed here, so a sales rep can never use this
     *                   to view a teammate's deals.
     */
    @GetMapping
    public ResponseEntity<List<Deal>> getAll(@RequestParam(required = false) String assignedTo) {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        if (orgId == null) {
            return ResponseEntity.ok(dealRepository.findAll());
        }
        if (owner != null) {
            return ResponseEntity.ok(dealRepository.findByOrganizationIdAndAssignedTo(orgId, owner));
        }
        if (assignedTo != null && !assignedTo.isBlank()) {
            return ResponseEntity.ok(dealRepository.findByOrganizationIdAndAssignedTo(orgId, assignedTo));
        }
        return ResponseEntity.ok(dealRepository.findByOrganizationId(orgId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Deal> getById(@PathVariable Long id) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(DEAL_NOT_FOUND));
        UserContextHelper.assertAccess(deal.getOrganizationId(), deal.getAssignedTo());
        return ResponseEntity.ok(deal);
    }

    @PostMapping
    public ResponseEntity<Deal> save(@RequestBody Deal deal) {
        if (deal.getId() != null) {
            Deal existing = dealRepository.findById(deal.getId())
                    .orElseThrow(() -> new NoSuchElementException(DEAL_NOT_FOUND));
            UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getAssignedTo());
            applyPatch(existing, deal);
            return ResponseEntity.ok(dealRepository.save(existing));
        }
        deal.setAssignedTo(UserContextHelper.currentUsername());
        deal.setOrganizationId(UserContextHelper.currentOrganizationId());
        return ResponseEntity.ok(dealRepository.save(deal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Deal> update(@PathVariable Long id, @RequestBody Deal deal) {
        Deal existing = dealRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(DEAL_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getAssignedTo());
        applyPatch(existing, deal);
        // Preserve original assignedTo — edit does not reassign
        return ResponseEntity.ok(dealRepository.save(existing));
    }

    private void applyPatch(Deal existing, Deal patch) {
        if (patch.getDealName() != null && !patch.getDealName().isBlank()) existing.setDealName(patch.getDealName());
        if (patch.getAccount() != null && !patch.getAccount().isBlank()) existing.setAccount(patch.getAccount());
        if (patch.getContact() != null) existing.setContact(patch.getContact());
        if (patch.getAmount() != null) existing.setAmount(patch.getAmount());
        if (patch.getStage() != null) existing.setStage(patch.getStage());
        if (patch.getProbability() != null) existing.setProbability(patch.getProbability());
        if (patch.getExpectedCloseDate() != null) existing.setExpectedCloseDate(patch.getExpectedCloseDate());
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Deal> approve(@PathVariable Long id) {
        Deal existing = dealRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(DEAL_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getAssignedTo());
        existing.setStage("Closed Won");
        return ResponseEntity.ok(dealRepository.save(existing));
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<Deal> reject(@PathVariable Long id) {
        Deal existing = dealRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(DEAL_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getAssignedTo());
        existing.setStage("Closed Lost");
        return ResponseEntity.ok(dealRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Deal existing = dealRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(DEAL_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getAssignedTo());
        dealRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Deal deleted successfully"));
    }

    // ── Related: Activities ──────────────────────────────────────────────────

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<Activity>> getActivities(@PathVariable Long id) {
        return ResponseEntity.ok(activityRepository.findByRelatedTypeIgnoreCaseAndRelatedId("Deal", id));
    }

    // ── Related: Tasks ───────────────────────────────────────────────────────

    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<CrmTask>> getTasks(@PathVariable Long id) {
        return ResponseEntity.ok(taskRepository.findByRelatedTypeIgnoreCaseAndRelatedId("Deal", id));
    }

    // ── Related: Quotes ──────────────────────────────────────────────────────

    @GetMapping("/{id}/quotes")
    public ResponseEntity<List<Quote>> getQuotes(@PathVariable Long id) {
        List<Quote> quotes = quoteRepository.findByDealId(id);
        if (quotes.isEmpty()) {
            Deal deal = dealRepository.findById(id).orElse(null);
            if (deal != null) {
                quotes = quoteRepository.findByAccountIgnoreCase(deal.getAccount());
            }
        }
        return ResponseEntity.ok(quotes);
    }

    @PostMapping("/{id}/quotes")
    public ResponseEntity<Quote> createQuoteFromDeal(@PathVariable Long id) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(DEAL_NOT_FOUND));
        UserContextHelper.assertAccess(deal.getOrganizationId(), deal.getAssignedTo());

        String currentUser = UserContextHelper.currentUsername();
        String quoteNumber = userSettingsService.getAndIncrementQuoteNumber();
        BigDecimal amount = deal.getAmount() != null ? deal.getAmount() : BigDecimal.ZERO;

        // A Deal has no line items to derive a subtotal from, but tax still must be
        // calculated against the org's actual GST/VAT configuration here — previously
        // this quote was saved with taxSystem/taxBreakdownJson left null, which made the
        // PDF fall back to a hardcoded flat 18% GST row regardless of the org's real
        // tax regime (see QuoteController/InvoiceController.applyTaxAndAmount for the
        // same fix on the normal creation path).
        OrganizationTaxSettings settings = taxSettingsService.findForOrg(deal.getOrganizationId());
        TaxBreakdown breakdown = taxCalculationService.calculate(settings, null, amount);
        String taxBreakdownJson;
        try {
            taxBreakdownJson = objectMapper.writeValueAsString(breakdown.getTaxes());
        } catch (Exception e) {
            taxBreakdownJson = null;
        }

        Quote quote = Quote.builder()
                .quoteNumber(quoteNumber)
                .contact(deal.getContact())
                .account(deal.getAccount())
                .amount(amount)
                .dealId(id)
                .createdBy(currentUser)
                .status("Draft")
                .organizationId(deal.getOrganizationId())
                .taxSystem(breakdown.getTaxSystem())
                .totalTax(breakdown.getTotalTax())
                .grandTotal(breakdown.getGrandTotal())
                .taxBreakdownJson(taxBreakdownJson)
                .build();

        return ResponseEntity.ok(quoteRepository.save(quote));
    }

    // ── Related: Invoices ────────────────────────────────────────────────────

    @GetMapping("/{id}/invoices")
    public ResponseEntity<List<Invoice>> getInvoices(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceRepository.findByDealId(id));
    }
}
