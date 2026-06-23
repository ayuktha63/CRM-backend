package com.orque.crm.audit.service;

import com.orque.crm.audit.entity.AuditLog;
import com.orque.crm.audit.repository.AuditLogRepository;
import com.orque.crm.enums.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl
        implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void createAudit(
            AuditAction action,
            String entityType,
            Long entityId,
            String description,
            String performedBy
    ) {

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .performedBy(performedBy)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}