package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.model.AuditLog;
import com.banksecurity.backend.repository.AuditLogRepository;
import com.banksecurity.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ==================== MÉTHODES D'ENREGISTREMENT ====================

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

    // ==================== MÉTHODES DE RECHERCHE DE BASE ====================

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
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
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
        log.info("Nettoyage des logs d'audit antérieurs au: {}", before);
    }

    @Override
    public long countLogs() {
        return auditLogRepository.count();
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT LES REPOSITORY ====================

    /**
     * Récupère les logs par entité (type + ID)
     */
    public List<AuditLog> getLogsByEntity(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Récupère les logs par utilisateur avec pagination
     */
    public Page<AuditLog> getLogsByUserPaginated(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Récupère les logs par utilisateur et période
     */
    public List<AuditLog> getLogsByUserAndDateRange(UUID userId, LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
    }

    /**
     * Récupère les logs par adresse IP
     */
    public List<AuditLog> getLogsByIpAddress(String ipAddress) {
        return auditLogRepository.findByIpAddress(ipAddress);
    }

    /**
     * Compte les actions par type depuis une date
     */
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

    /**
     * Recherche avancée avec filtres combinés et pagination
     */
    public Page<AuditLog> getLogsWithFilters(
            UUID userId,
            String action,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return auditLogRepository.findLogsWithFilters(userId, action, startDate, endDate, pageable);
    }

    /**
     * Récupère les actions les plus fréquentes
     */
    public Map<String, Long> getTopActions(int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
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

    /**
     * Récupère les logs d'une entité spécifique
     */
    public List<AuditLog> getEntityHistory(String entityType, UUID entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Vérifie si une adresse IP est suspecte (plus de 10 actions échouées)
     */
    public boolean isIpSuspicious(String ipAddress) {
        List<AuditLog> logs = auditLogRepository.findByIpAddress(ipAddress);
        long failedCount = logs.stream()
                .filter(log -> !log.getSuccess())
                .count();
        return failedCount >= 10;
    }
}