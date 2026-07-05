package com.orque.crm.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * audit_logs.action/module were originally created with hand-written Postgres CHECK
 * constraints listing every valid enum value literally. ddl-auto=update never touches
 * existing constraints, so every time someone added a new AuditAction (e.g. LEAD_DELETED)
 * the constraint silently went stale — any insert using the new value failed with
 * "violates check constraint audit_logs_action_check", which is exactly what broke lead
 * deletion (auditLogService.createAudit(LEAD_DELETED, ...) ran inside the same delete
 * transaction, so the whole delete rolled back).
 *
 * The entity already maps `action`/`module` via @Enumerated(EnumType.STRING) — the Java
 * enum is the real source of truth and only ever writes valid values. The DB-level
 * CHECK constraints are redundant and were the actual bug (never kept in sync), so they're
 * dropped here rather than patched with the current enum values, which would just leave
 * the same footgun for the next new enum constant. Runs once per app start, idempotent
 * (IF EXISTS), so every environment self-heals on next deploy without a manual DB session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogSchemaFixup implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            jdbcTemplate.execute("ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check");
            jdbcTemplate.execute("ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_module_check");
        } catch (Exception e) {
            log.warn("Could not drop stale audit_logs CHECK constraints (non-fatal): {}", e.getMessage());
        }
    }
}
