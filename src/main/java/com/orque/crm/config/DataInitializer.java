package com.orque.crm.config;

import com.orque.crm.auth.entity.Role;
import com.orque.crm.auth.entity.User;
import com.orque.crm.auth.repository.RoleRepository;
import com.orque.crm.auth.repository.UserRepository;
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
import com.orque.crm.lead.entity.Lead;
import com.orque.crm.lead.repository.LeadRepository;
import com.orque.crm.task.entity.CrmTask;
import com.orque.crm.task.repository.CrmTaskRepository;
import com.orque.crm.campaign.entity.Campaign;
import com.orque.crm.campaign.entity.CampaignMetrics;
import com.orque.crm.campaign.repository.CampaignRepository;
import com.orque.crm.campaign.repository.CampaignMetricsRepository;
import com.orque.crm.enums.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final CrmTaskRepository taskRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignMetricsRepository campaignMetricsRepository;
    private final AccountRepository accountRepository;
    private final DealRepository dealRepository;
    private final ActivityRepository activityRepository;
    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;

    private static final String ADMIN = "admin";

    @Override
    public void run(String... args) {

        if (roleRepository.findByName(RoleType.ADMIN).isEmpty()) {
            Role adminRole = Role.builder()
                    .name(RoleType.ADMIN)
                    .build();

            roleRepository.save(adminRole);
        }

        if (roleRepository.findByName(RoleType.SALES_USER).isEmpty()) {
            roleRepository.save(Role.builder().name(RoleType.SALES_USER).build());
        }
        if (roleRepository.findByName(RoleType.SALES_ADMIN).isEmpty()) {
            roleRepository.save(Role.builder().name(RoleType.SALES_ADMIN).build());
        }
        if (roleRepository.findByName(RoleType.SALES).isEmpty()) {
            roleRepository.save(Role.builder().name(RoleType.SALES).build());
        }

        if (!userRepository.existsByUsername(ADMIN)) {

            Role adminRole = roleRepository
                    .findByName(RoleType.ADMIN)
                    .orElseThrow(() ->
                            new RuntimeException("Admin role not found"));

            User admin = User.builder()
                    .firstName("System")
                    .lastName("Administrator")
                    .username(ADMIN)
                    .email("admin@orque.io")
                    .password(passwordEncoder.encode("Admin@123"))
                    .enabled(true)
                    .status("ACTIVE")
                    .role(adminRole)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
        } else {
            userRepository.findByUsername(ADMIN).ifPresent(user -> {
                if ("admin@orque.com".equals(user.getEmail())) {
                    user.setEmail("admin@orque.io");
                    userRepository.save(user);
                }
            });
        }

        if (!userRepository.existsByUsername("demo")) {

            Role salesRole = roleRepository
                    .findByName(RoleType.SALES_USER)
                    .orElseThrow(() ->
                            new RuntimeException("Sales role not found"));

            User demo = User.builder()
                    .firstName("Demo")
                    .lastName("Sales")
                    .username("demo")
                    .email("demo@orque.io")
                    .password(
                            passwordEncoder.encode("Admin@123")
                    )
                    .enabled(true)
                    .role(salesRole)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            userRepository.save(demo);
        }

        // Seed contacts
        if (contactRepository.count() == 0) {
            Contact contact1 = Contact.builder()
                    .fullName("Alice Johnson")
                    .company("Acme Corp")
                    .email("alice@acme.com")
                    .phone("+1-555-0199")
                    .jobTitle("Purchasing Manager")
                    .industry("Technology")
                    .status(ContactStatus.NEW)
                    .owner(ADMIN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            contactRepository.save(contact1);

            Contact contact2 = Contact.builder()
                    .fullName("Bob Smith")
                    .company("Global Industries")
                    .email("bob@globalind.com")
                    .phone("+1-555-0142")
                    .jobTitle("VP of Sales")
                    .industry("Manufacturing")
                    .status(ContactStatus.REVIEWED)
                    .owner(ADMIN)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now())
                    .build();
            contactRepository.save(contact2);
        }

        // Seed leads
        if (leadRepository.count() == 0) {
            Lead lead1 = Lead.builder()
                    .fullName("Charlie Brown")
                    .company("Snoopy Consulting")
                    .email("charlie@snoopy.com")
                    .phone("+1-555-0187")
                    .jobTitle("Operations Lead")
                    .industry("Consulting")
                    .leadSource(LeadSource.WEBSITE)
                    .assignedOwner(ADMIN)
                    .status(LeadStatus.NEW)
                    .pipelineStage(PipelineStage.NEW)
                    .estimatedValue(new BigDecimal("120000")) // 1.2 Lakhs
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .updatedAt(LocalDateTime.now())
                    .build();
            leadRepository.save(lead1);

            Lead lead2 = Lead.builder()
                    .fullName("Diana Prince")
                    .company("Themyscira Ltd")
                    .email("diana@themyscira.io")
                    .phone("+1-555-0111")
                    .jobTitle("Managing Director")
                    .industry("Other")
                    .leadSource(LeadSource.REFERRAL)
                    .assignedOwner(ADMIN)
                    .status(LeadStatus.QUALIFIED)
                    .pipelineStage(PipelineStage.NEGOTIATION)
                    .estimatedValue(new BigDecimal("45000000")) // 4.5 Crore
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .updatedAt(LocalDateTime.now())
                    .build();
            leadRepository.save(lead2);
        }

        // Seed tasks
        if (taskRepository.count() == 0) {
            CrmTask task1 = CrmTask.builder()
                    .title("Initial Intro Call")
                    .description("Call Snoopy Consulting Operations Lead to introduce our product.")
                    .taskType(TaskType.CALL)
                    .priority(TaskPriority.HIGH)
                    .status(TaskStatus.PENDING)
                    .assignedTo(ADMIN)
                    .dueDate(LocalDateTime.now().plusDays(1))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            taskRepository.save(task1);

            CrmTask task2 = CrmTask.builder()
                    .title("Proposal Review Meeting")
                    .description("Go over the customized pricing model for Themyscira Ltd.")
                    .taskType(TaskType.MEETING)
                    .priority(TaskPriority.CRITICAL)
                    .status(TaskStatus.IN_PROGRESS)
                    .assignedTo(ADMIN)
                    .dueDate(LocalDateTime.now()) // Due today
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .updatedAt(LocalDateTime.now())
                    .build();
            taskRepository.save(task2);
        }

        // Seed campaigns
        if (campaignRepository.count() == 0) {
            Campaign campaign = Campaign.builder()
                    .campaignName("Q2 Enterprise Outreach")
                    .subjectLine("Transform your backend operations with Orque CRM")
                    .emailBody("Hello, we are introducing our new CRM module to simplify and secure your tenant workflow...")
                    .status(CampaignStatus.RUNNING)
                    .createdAt(LocalDateTime.now().minusDays(15))
                    .build();
            Campaign savedCampaign = campaignRepository.save(campaign);

            CampaignMetrics metrics = CampaignMetrics.builder()
                    .campaignId(savedCampaign.getId())
                    .totalRecipients(150)
                    .sentCount(148)
                    .failedCount(2)
                    .deliveredCount(145)
                    .openedCount(98)
                    .repliedCount(42)
                    .build();
            campaignMetricsRepository.save(metrics);
        }

        // One-time migration: assign 'admin' ownership to all records created before
        // the ownership system was introduced (owner/assignedOwner/assignedTo/createdBy = null).
        // Runs on every startup but is effectively a no-op once all records have owners.
        migrateNullOwners();
    }

    private void migrateNullOwners() {
        contactRepository.findAll().stream()
                .filter(c -> c.getOwner() == null || c.getOwner().isBlank())
                .forEach(c -> { c.setOwner(ADMIN); contactRepository.save(c); });

        leadRepository.findAll().stream()
                .filter(l -> l.getAssignedOwner() == null || l.getAssignedOwner().isBlank())
                .forEach(l -> { l.setAssignedOwner(ADMIN); leadRepository.save(l); });

        accountRepository.findAll().stream()
                .filter(a -> a.getOwner() == null || a.getOwner().isBlank())
                .forEach(a -> { a.setOwner(ADMIN); accountRepository.save(a); });

        dealRepository.findAll().stream()
                .filter(d -> d.getAssignedTo() == null || d.getAssignedTo().isBlank())
                .forEach(d -> { d.setAssignedTo(ADMIN); dealRepository.save(d); });

        activityRepository.findAll().stream()
                .filter(a -> a.getAssignedTo() == null || a.getAssignedTo().isBlank())
                .forEach(a -> { a.setAssignedTo(ADMIN); activityRepository.save(a); });

        taskRepository.findAll().stream()
                .filter(t -> t.getAssignedTo() == null || t.getAssignedTo().isBlank())
                .forEach(t -> { t.setAssignedTo(ADMIN); taskRepository.save(t); });

        quoteRepository.findAll().stream()
                .filter(q -> q.getCreatedBy() == null || q.getCreatedBy().isBlank())
                .forEach(q -> { q.setCreatedBy(ADMIN); quoteRepository.save(q); });

        invoiceRepository.findAll().stream()
                .filter(i -> i.getCreatedBy() == null || i.getCreatedBy().isBlank())
                .forEach(i -> { i.setCreatedBy(ADMIN); invoiceRepository.save(i); });
    }
}
