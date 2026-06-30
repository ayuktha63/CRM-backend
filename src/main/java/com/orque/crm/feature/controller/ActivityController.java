package com.orque.crm.feature.controller;

import com.orque.crm.auth.entity.User;
import com.orque.crm.common.UserContextHelper;
import com.orque.crm.contact.repository.ContactRepository;
import com.orque.crm.feature.entity.Account;
import com.orque.crm.feature.entity.Activity;
import com.orque.crm.feature.entity.Deal;
import com.orque.crm.feature.repository.AccountRepository;
import com.orque.crm.feature.repository.ActivityRepository;
import com.orque.crm.feature.repository.DealRepository;
import com.orque.crm.timeline.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@CrossOrigin
public class ActivityController {

    private final ActivityRepository activityRepository;
    private final ContactRepository contactRepository;
    private final DealRepository dealRepository;
    private final AccountRepository accountRepository;
    private final TimelineService timelineService;

    @GetMapping
    public ResponseEntity<List<Activity>> getAll() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Activity> activities = activityRepository.findAll();
        if (principal instanceof User u) {
            String roleName = u.getRole() != null ? u.getRole().getName().name() : "";
            if (!"ADMIN".equals(roleName) && !"SALES_ADMIN".equals(roleName)) {
                return ResponseEntity.ok(activities.stream()
                        .filter(a -> a.getAssignedTo() == null || a.getAssignedTo().trim().isEmpty() || a.getAssignedTo().equalsIgnoreCase(u.getUsername()))
                        .toList());
            }
        }
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activity> getById(@PathVariable Long id) {
        return activityRepository.findById(id)
                .map(a -> {
                    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (principal instanceof User u) {
                        String roleName = u.getRole() != null ? u.getRole().getName().name() : "";
                        if (!"ADMIN".equals(roleName) && !"SALES_ADMIN".equals(roleName)) {
                            if (a.getAssignedTo() != null && !a.getAssignedTo().equalsIgnoreCase(u.getUsername())) {
                                return ResponseEntity.status(403).<Activity>build();
                            }
                        }
                    }
                    return ResponseEntity.ok(a);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-record")
    public ResponseEntity<List<Activity>> getByRecord(
            @RequestParam String type,
            @RequestParam Long id) {
        List<Activity> activities = activityRepository.findByRelatedTypeIgnoreCaseAndRelatedId(type, id);
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User u) {
            String roleName = u.getRole() != null ? u.getRole().getName().name() : "";
            if (!"ADMIN".equals(roleName) && !"SALES_ADMIN".equals(roleName)) {
                return ResponseEntity.ok(activities.stream()
                        .filter(a -> a.getAssignedTo() == null || a.getAssignedTo().trim().isEmpty() || a.getAssignedTo().equalsIgnoreCase(u.getUsername()))
                        .toList());
            }
        }
        return ResponseEntity.ok(activities);
    }

    @PostMapping
    public ResponseEntity<Activity> save(@RequestBody Activity activity) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUsername = (principal instanceof User u) ? u.getUsername() : "admin";

        resolveRelatedId(activity);
        if (activity.getId() != null) {
            Activity existing = activityRepository.findById(activity.getId())
                    .orElseThrow(() -> new RuntimeException("Activity not found"));
            UserContextHelper.assertAccess(existing.getAssignedTo());
            existing.setType(activity.getType());
            existing.setSubject(activity.getSubject());
            existing.setContact(activity.getContact());
            // Preserve original owner — edit does not reassign
            existing.setDuration(activity.getDuration());
            existing.setDueDate(activity.getDueDate());
            existing.setStatus(activity.getStatus());
            existing.setRelatedType(activity.getRelatedType());
            existing.setRelatedName(activity.getRelatedName());
            existing.setRelatedId(activity.getRelatedId());
            Activity saved = activityRepository.save(existing);
            logActivityToTimelines(saved, "Activity Updated");
            return ResponseEntity.ok(saved);
        }
        activity.setAssignedTo(currentUsername);
        Activity saved = activityRepository.save(activity);
        logActivityToTimelines(saved, "Activity Logged");
        return ResponseEntity.ok(saved);
    }

    private void resolveRelatedId(Activity a) {
        if (a.getRelatedId() != null) return;
        String type = a.getRelatedType();
        String name = a.getRelatedName() != null ? a.getRelatedName() : a.getContact();
        if (type == null || name == null || name.isBlank()) return;
        switch (type.toLowerCase()) {
            case "contact" -> contactRepository.findByEmailIgnoreCase(name)
                    .or(() -> contactRepository.findAll().stream()
                            .filter(c -> c.getFullName().equalsIgnoreCase(name)).findFirst())
                    .ifPresent(c -> a.setRelatedId(c.getId()));
            case "deal"    -> dealRepository.findAll().stream()
                    .filter(d -> d.getDealName().equalsIgnoreCase(name)).findFirst()
                    .ifPresent(d -> a.setRelatedId(d.getId()));
            case "account" -> accountRepository.findByCompanyNameIgnoreCase(name)
                    .ifPresent(acc -> a.setRelatedId(acc.getId()));
            default -> { /* Lead and other types have no dedicated lookup — relatedId remains null */ }
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Activity> update(@PathVariable Long id, @RequestBody Activity activity) {
        Activity existing = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        UserContextHelper.assertAccess(existing.getAssignedTo());
        existing.setType(activity.getType());
        existing.setSubject(activity.getSubject());
        existing.setContact(activity.getContact());
        // Preserve original owner — edit does not reassign
        existing.setDuration(activity.getDuration());
        existing.setDueDate(activity.getDueDate());
        existing.setStatus(activity.getStatus());
        existing.setRelatedType(activity.getRelatedType());
        existing.setRelatedName(activity.getRelatedName());
        existing.setRelatedId(activity.getRelatedId());
        Activity saved = activityRepository.save(existing);
        logActivityToTimelines(saved, "Activity Updated");
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Activity> approve(@PathVariable Long id) {
        Activity existing = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        existing.setStatus("Completed");
        Activity saved = activityRepository.save(existing);
        logActivityToTimelines(saved, "Activity Completed");
        return ResponseEntity.ok(saved);
    }

    private void logActivityToTimelines(Activity activity, String action) {
        try {
            String desc = activity.getType() + ": " + activity.getSubject() + 
                         (activity.getStatus() != null ? " (" + activity.getStatus() + ")" : "");
            
            // 1. Log to the primary related entity's timeline
            if (activity.getRelatedType() != null && activity.getRelatedId() != null) {
                String module = activity.getRelatedType().toLowerCase();
                if ("contact".equals(module)) module = "contacts";
                else if ("account".equals(module)) module = "accounts";
                else if ("deal".equals(module)) module = "deals";
                else if ("lead".equals(module)) module = "leads";
                
                timelineService.record(module, activity.getRelatedId(), action, desc);
            }

            // 2. Log to the contact's timeline if logged for a different type and we have a contact name/email
            boolean isDirectContact = "contact".equalsIgnoreCase(activity.getRelatedType());
            if (!isDirectContact && activity.getContact() != null && !activity.getContact().isBlank()) {
                String contactNameOrEmail = activity.getContact();
                contactRepository.findByEmailIgnoreCase(contactNameOrEmail)
                        .or(() -> contactRepository.findAll().stream()
                                .filter(c -> c.getFullName().equalsIgnoreCase(contactNameOrEmail)).findFirst())
                        .ifPresent(c -> {
                            timelineService.record("contacts", c.getId(), action, desc + " on related " + activity.getRelatedType() + " '" + activity.getRelatedName() + "'");
                        });
            }
        } catch (Exception e) {
            System.err.println("Failed to log activity timeline: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        activityRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Activity deleted successfully"));
    }
}
