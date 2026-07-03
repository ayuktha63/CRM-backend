package com.orque.crm.feature.controller;

import com.orque.crm.common.UserContextHelper;
import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.feature.entity.Account;
import com.orque.crm.feature.entity.Activity;
import com.orque.crm.feature.entity.Deal;
import com.orque.crm.feature.entity.Invoice;
import com.orque.crm.feature.entity.Quote;
import com.orque.crm.feature.repository.AccountRepository;
import com.orque.crm.feature.repository.ActivityRepository;
import com.orque.crm.feature.repository.DealRepository;
import com.orque.crm.feature.repository.InvoiceRepository;
import com.orque.crm.feature.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@CrossOrigin
public class AccountController {

    private static final String ACCOUNT_NOT_FOUND = "Account not found";

    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final ActivityRepository activityRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping
    public ResponseEntity<List<Account>> getAll() {
        String orgId = UserContextHelper.scopedOrgId();
        String owner = UserContextHelper.scopedOwner();
        if (orgId == null) {
            return ResponseEntity.ok(accountRepository.findAll());
        }
        if (owner == null) {
            return ResponseEntity.ok(accountRepository.findByOrganizationId(orgId));
        }
        return ResponseEntity.ok(accountRepository.findByOrganizationIdAndOwner(orgId, owner));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(a -> {
                    UserContextHelper.assertAccess(a.getOrganizationId(), a.getOwner());
                    return ResponseEntity.ok(a);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Account> save(@RequestBody Account account) {
        if (account.getId() != null) {
            Account existing = accountRepository.findById(account.getId())
                    .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
            UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getOwner());
            existing.setCompanyName(account.getCompanyName());
            existing.setIndustry(account.getIndustry());
            existing.setPhone(account.getPhone());
            existing.setWebsite(account.getWebsite());
            existing.setEmployees(account.getEmployees());
            existing.setAnnualRevenue(account.getAnnualRevenue());
            existing.setCountry(account.getCountry());
            existing.setStatus(account.getStatus());
            return ResponseEntity.ok(accountRepository.save(existing));
        }
        account.setOwner(UserContextHelper.currentUsername());
        account.setOrganizationId(UserContextHelper.currentOrganizationId());
        return ResponseEntity.ok(accountRepository.save(account));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> update(@PathVariable Long id, @RequestBody Account account) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getOwner());
        existing.setCompanyName(account.getCompanyName());
        existing.setIndustry(account.getIndustry());
        existing.setPhone(account.getPhone());
        existing.setWebsite(account.getWebsite());
        existing.setEmployees(account.getEmployees());
        existing.setAnnualRevenue(account.getAnnualRevenue());
        existing.setCountry(account.getCountry());
        existing.setStatus(account.getStatus());
        return ResponseEntity.ok(accountRepository.save(existing));
    }

    @PostMapping("/deactivate/{id}")
    public ResponseEntity<Account> deactivate(@PathVariable Long id) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getOwner());
        existing.setStatus("Inactive");
        return ResponseEntity.ok(accountRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        Account existing = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
        UserContextHelper.assertAccess(existing.getOrganizationId(), existing.getOwner());
        accountRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Account deleted successfully"));
    }

    // ── Related records ──────────────────────────────────────────────────────

    @GetMapping("/{id}/contacts")
    public ResponseEntity<List<Contact>> getContacts(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
        return ResponseEntity.ok(contactRepository.findByCompanyIgnoreCase(account.getCompanyName()));
    }

    @GetMapping("/{id}/deals")
    public ResponseEntity<List<Deal>> getDeals(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
        return ResponseEntity.ok(dealRepository.findByAccountIgnoreCase(account.getCompanyName()));
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<Activity>> getActivities(@PathVariable Long id) {
        return ResponseEntity.ok(
                activityRepository.findByRelatedTypeIgnoreCaseAndRelatedId("Account", id));
    }

    @GetMapping("/{id}/quotes")
    public ResponseEntity<List<Quote>> getQuotes(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
        return ResponseEntity.ok(quoteRepository.findByAccountIgnoreCase(account.getCompanyName()));
    }

    @GetMapping("/{id}/invoices")
    public ResponseEntity<List<Invoice>> getInvoices(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(ACCOUNT_NOT_FOUND));
        return ResponseEntity.ok(invoiceRepository.findByAccountIgnoreCase(account.getCompanyName()));
    }
}
