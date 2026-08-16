package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.model.AuditLog;
import com.banksecurity.backend.repository.AuditLogRepository;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.util.AsyncUtils;
import com.banksecurity.backend.util.Constants;
import com.banksecurity.backend.util.DateUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Resource(name = "auditExecutor")
    private Executor auditExecutor;

    // ==================== MÉTHODES D'ENREGISTREMENT ====================

    @Override
    @Transactional
    public void logAction(UUID userId, String action, String details) {
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> {
                    AuditLog auditLog = AuditLog.builder()
                            .userId(userId)
                            .action(action)
                            .details(details)
                            .success(true)
                            .build();
                    auditLogRepository.save(auditLog);
                },
                auditExecutor,
                "Audit action: " + action
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'audit: {}", e.getMessage());
            return null;
        });
        log.debug("Action auditée (async): {}", action);
    }

    @Override
    @Transactional
    public void logAction(UUID userId, String action, String details, String ipAddress, String userAgent) {
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> {
                    AuditLog auditLog = AuditLog.builder()
                            .userId(userId)
                            .action(action)
                            .details(details)
                            .ipAddress(ipAddress)
                            .userAgent(userAgent)
                            .success(true)
                            .build();
                    auditLogRepository.save(auditLog);
                },
                auditExecutor,
                "Audit action avec IP: " + action
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'audit avec IP: {}", e.getMessage());
            return null;
        });
        log.debug("Action auditée avec IP (async): {}", action);
    }

    @Override
    @Transactional
    public void logEntityAction(UUID userId, String action, String entityType, UUID entityId, String details) {
        CompletableFuture<Void> future = AsyncUtils.runAsync(
                () -> {
                    AuditLog auditLog = AuditLog.builder()
                            .userId(userId)
                            .action(action)
                            .entityType(entityType)
                            .entityId(entityId)
                            .details(details)
                            .success(true)
                            .build();
                    auditLogRepository.save(auditLog);
                },
                auditExecutor,
                "Audit entité: " + action + " sur " + entityType
        );
        future.exceptionally(e -> {
            log.error("Erreur lors de l'audit entité: {}", e.getMessage());
            return null;
        });
        log.debug("Action entité auditée (async): {} sur {}", action, entityType);
    }

    // ==================== MÉTHODES DE RECHERCHE DE BASE ====================

    @Override
    public List<AuditLog> getLogsByUser(UUID userId) {
        // ✅ Utilisation de Constants.AUDIT_ACTION_VIEW
        logAction(userId, Constants.AUDIT_ACTION_VIEW, "Consultation des logs utilisateur");
        return auditLogRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    @Override
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        if (DateUtils.isFuture(start) || DateUtils.isFuture(end)) {
            throw new IllegalArgumentException("Les dates ne peuvent pas être dans le futur");
        }
        return auditLogRepository.findByCreatedAtBetween(start, end);
    }

    @Override
    public List<AuditLog> getRecentLogs(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Constants.DEFAULT_SORT_FIELD).descending());
        return auditLogRepository.findRecentLogs(pageable);
    }

    @Override
    public List<AuditLog> getFailedActions() {
        return auditLogRepository.findBySuccessFalse();
    }

    @Override
    @Transactional
    public void cleanupOldLogs(LocalDateTime before) {
        auditLogRepository.deleteByCreatedAtBefore(before);
        log.info("Nettoyage des logs d'audit antérieurs au: {}", DateUtils.format(before));
    }

    @Override
    public long countLogs() {
        return auditLogRepository.count();
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT LES REPOSITORY ====================

    public List<AuditLog> getLogsByEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public Page<AuditLog> getLogsByUserPaginated(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Constants.DEFAULT_SORT_FIELD).descending());
        return auditLogRepository.findByUserId(userId, pageable);
    }

    public List<AuditLog> getLogsByUserAndDateRange(UUID userId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
    }

    public List<AuditLog> getLogsByIpAddress(String ipAddress) {
        return auditLogRepository.findByIpAddress(ipAddress);
    }

    public Map<String, Long> countActionsSince(LocalDateTime since) {
        List<Object[]> results = auditLogRepository.countActionsSince(since);
        Map<String, Long> actionCounts = new HashMap<>();

        for (Object[] result : results) {
            if (result.length >= 2 && result[0] != null && result[1] != null) {
                actionCounts.put(result[0].toString(), Long.parseLong(result[1].toString()));
            }
        }

        return actionCounts;
    }

    public Page<AuditLog> getLogsWithFilters(
            UUID userId,
            String action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Constants.DEFAULT_SORT_FIELD).descending());
        return auditLogRepository.findLogsWithFilters(userId, action, startDate, endDate, pageable);
    }

    public Map<String, Long> getTopActions(int limit) {
        LocalDateTime since = DateUtils.daysAgo(Constants.DEFAULT_ALERT_RETENTION_DAYS);
        return countActionsSince(since).entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        HashMap::new
                ));
    }

    public List<AuditLog> getEntityHistory(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public boolean isIpSuspicious(String ipAddress) {
        List<AuditLog> logs = auditLogRepository.findByIpAddress(ipAddress);
        long failedCount = logs.stream()
                .filter(log -> !log.getSuccess())
                .count();
        return failedCount >= 10;
    }
}