package com.orque.crm.feature.controller;

import com.orque.crm.contact.entity.Contact;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.feature.entity.Account;
import com.orque.crm.feature.entity.Deal;
import com.orque.crm.feature.repository.AccountRepository;
import com.orque.crm.feature.repository.DealRepository;
import com.orque.crm.lead.entity.Lead;
import com.orque.crm.lead.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
@CrossOrigin
public class BulkController {

    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;
    private final AccountRepository accountRepository;
    private final DealRepository dealRepository;

    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> bulkDelete(
            @RequestParam String module,
            @RequestBody List<Long> ids) {
        
        switch (module.toLowerCase()) {
            case "leads" -> leadRepository.deleteAllById(ids);
            case "contacts" -> contactRepository.deleteAllById(ids);
            case "accounts" -> accountRepository.deleteAllById(ids);
            case "deals" -> dealRepository.deleteAllById(ids);
            default -> throw new RuntimeException("Invalid module: " + module);
        }
        
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("count", ids.size());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> bulkAssign(
            @RequestParam String module,
            @RequestParam String owner,
            @RequestBody List<Long> ids) {
        
        switch (module.toLowerCase()) {
            case "leads" -> {
                List<Lead> items = leadRepository.findAllById(ids);
                items.forEach(i -> i.setAssignedOwner(owner));
                leadRepository.saveAll(items);
            }
            case "contacts" -> {
                List<Contact> items = contactRepository.findAllById(ids);
                items.forEach(i -> i.setOwner(owner));
                contactRepository.saveAll(items);
            }
            case "accounts" -> {
                List<Account> items = accountRepository.findAllById(ids);
                items.forEach(i -> i.setOwner(owner));
                accountRepository.saveAll(items);
            }
            case "deals" -> {
                List<Deal> items = dealRepository.findAllById(ids);
                items.forEach(i -> i.setAssignedTo(owner));
                dealRepository.saveAll(items);
            }
            default -> throw new RuntimeException("Invalid module: " + module);
        }
        
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("count", ids.size());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> bulkStatusUpdate(
            @RequestParam String module,
            @RequestParam String status,
            @RequestBody List<Long> ids) {
        
        switch (module.toLowerCase()) {
            case "leads" -> {
                List<Lead> items = leadRepository.findAllById(ids);
                try {
                    com.orque.crm.enums.LeadStatus enumVal = com.orque.crm.enums.LeadStatus.valueOf(status.toUpperCase());
                    items.forEach(i -> i.setStatus(enumVal));
                    leadRepository.saveAll(items);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid Lead Status: " + status);
                }
            }
            case "deals" -> {
                List<Deal> items = dealRepository.findAllById(ids);
                items.forEach(i -> i.setStage(status));
                dealRepository.saveAll(items);
            }
            case "accounts" -> {
                List<Account> items = accountRepository.findAllById(ids);
                items.forEach(i -> i.setStatus(status));
                accountRepository.saveAll(items);
            }
            default -> throw new RuntimeException("Status update not supported for module: " + module);
        }
        
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("count", ids.size());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/edit")
    public ResponseEntity<Map<String, Object>> bulkEdit(
            @RequestParam String module,
            @RequestParam String fieldName,
            @RequestParam String fieldValue,
            @RequestBody List<Long> ids) {
        
        switch (module.toLowerCase()) {
            case "leads" -> {
                List<Lead> items = leadRepository.findAllById(ids);
                for (Lead lead : items) {
                    setLeadField(lead, fieldName, fieldValue);
                }
                leadRepository.saveAll(items);
            }
            case "contacts" -> {
                List<Contact> items = contactRepository.findAllById(ids);
                for (Contact contact : items) {
                    setContactField(contact, fieldName, fieldValue);
                }
                contactRepository.saveAll(items);
            }
            case "accounts" -> {
                List<Account> items = accountRepository.findAllById(ids);
                for (Account account : items) {
                    setAccountField(account, fieldName, fieldValue);
                }
                accountRepository.saveAll(items);
            }
            case "deals" -> {
                List<Deal> items = dealRepository.findAllById(ids);
                for (Deal deal : items) {
                    setDealField(deal, fieldName, fieldValue);
                }
                dealRepository.saveAll(items);
            }
            default -> throw new RuntimeException("Bulk edit not supported for module: " + module);
        }
        
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("count", ids.size());
        return ResponseEntity.ok(resp);
    }

    private void setLeadField(Lead lead, String field, String val) {
        switch (field.toLowerCase()) {
            case "industry" -> lead.setIndustry(val);
            case "city" -> lead.setState(val);
            case "country" -> lead.setCountry(val);
            case "address" -> lead.setAddress(val);
            case "jobtitle" -> lead.setJobTitle(val);
            case "website" -> lead.setWebsite(val);
            default -> throw new RuntimeException("Field not supported for bulk edit: " + field);
        }
    }

    private void setContactField(Contact contact, String field, String val) {
        switch (field.toLowerCase()) {
            case "industry" -> contact.setIndustry(val);
            case "city" -> contact.setCity(val);
            case "country" -> contact.setCountry(val);
            case "address" -> contact.setAddress(val);
            case "jobtitle" -> contact.setJobTitle(val);
            case "website" -> contact.setWebsite(val);
            default -> throw new RuntimeException("Field not supported for bulk edit: " + field);
        }
    }

    private void setAccountField(Account account, String field, String val) {
        switch (field.toLowerCase()) {
            case "industry" -> account.setIndustry(val);
            case "phone" -> account.setPhone(val);
            case "country" -> account.setCountry(val);
            case "website" -> account.setWebsite(val);
            default -> throw new RuntimeException("Field not supported for bulk edit: " + field);
        }
    }

    private void setDealField(Deal deal, String field, String val) {
        switch (field.toLowerCase()) {
            case "stage" -> deal.setStage(val);
            case "amount" -> deal.setAmount(new java.math.BigDecimal(val));
            default -> throw new RuntimeException("Field not supported for bulk edit: " + field);
        }
    }
}
