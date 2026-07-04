-- Performance indexes for CRM (orque_crm database)
-- CRM runs with spring.jpa.hibernate.ddl-auto=update, so Hibernate will normally
-- add these automatically on next backend restart. This script is provided so the
-- indexes can also be applied immediately without waiting for a restart.
--
-- Usage: psql -U <user> -d orque_crm -f 001_add_performance_indexes.sql

CREATE INDEX IF NOT EXISTS idx_users_organization_id        ON users (organization_id);
CREATE INDEX IF NOT EXISTS idx_contacts_organization_id      ON contacts (organization_id);
CREATE INDEX IF NOT EXISTS idx_crm_licenses_organization_id  ON crm_licenses (organization_id);
CREATE INDEX IF NOT EXISTS idx_leads_organization_id         ON leads (organization_id);
