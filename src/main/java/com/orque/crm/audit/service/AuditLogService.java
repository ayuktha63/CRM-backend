package com.orque.crm.audit.service;

import com.orque.crm.enums.AuditAction;

public interface AuditLogService {

    void createAudit(
            AuditAction action,
            String entityType,
            Long entityId,
            String description,
            String performedBy
    );
}