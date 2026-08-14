package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.model.AuditLog;
import com.banksecurity.backend.repository.AuditLogRepository;
import com.banksecurity.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async("auditExecutor")
    @Transactional
    public void logAction(UUID userId, String action, String details) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .success(true)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Action auditée: {}", action);
    }

    @Override
    @Async("auditExecutor")
    @Transactional
    public void logAction(UUID userId, String action, String details, String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Action auditée avec IP: {}", action);
    }

    @Override
    @Async("auditExecutor")
    @Transactional
    public void logEntityAction(UUID userId, String action, String entityType, UUID entityId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .success(true)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Action entité auditée: {} sur {}", action, entityType);
    }

    @Override
    public List<AuditLog> getLogsByUser(UUID userId) {
        return auditLogRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    @Override
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByCreatedAtBetween(start, end);
    }

    @Override
    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findRecentLogs(
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    @Override
    public List<AuditLog> getFailedActions() {
        return auditLogRepository.findBySuccessFalse();
    }

    @Override
    @Transactional
    public void cleanupOldLogs(LocalDateTime before) {
        auditLogRepository.deleteByCreatedAtBefore(before);
        log.info("Nettoyage des logs d'audit antérieurs au: {}", before);
    }

    @Override
    public long countLogs() {
        return auditLogRepository.count();
    }
}