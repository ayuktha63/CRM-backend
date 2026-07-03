package com.orque.crm.audit.service;

import com.orque.crm.audit.entity.AuditLog;
import com.orque.crm.audit.repository.AuditLogRepository;
import com.orque.crm.enums.AuditAction;
import com.orque.crm.enums.AuditModule;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl
        implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void createAudit(
            AuditAction action,
            AuditModule module,
            String entityType,
            Long entityId,
            String previousValue,
            String newValue,
            String description,
            String performedBy,
            String ipAddress
    ) {

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .module(module)
                .entityType(entityType)
                .entityId(entityId)
                .previousValue(previousValue)
                .newValue(newValue)
                .description(description)
                .performedBy(performedBy)
                .ipAddress(ipAddress)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}