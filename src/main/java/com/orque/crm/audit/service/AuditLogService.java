package com.orque.crm.audit.service;

import com.orque.crm.enums.AuditAction;
import com.orque.crm.enums.AuditModule;

public interface AuditLogService {

    void createAudit(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            String previousValue,
            String newValue,
            String description,
            String performedBy,
            String ipAddress
    );
}