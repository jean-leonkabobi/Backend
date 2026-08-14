package com.banksecurity.backend.repository;

import com.banksecurity.backend.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Recherche par utilisateur
    List<AuditLog> findByUserId(UUID userId);

    // Recherche par action
    List<AuditLog> findByAction(String action);

    // Recherche par entité
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    // Recherche par période
    List<AuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Recherche des actions échouées
    List<AuditLog> findBySuccessFalse();

    // Recherche paginée
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    // Recherche des logs récents
    @Query("SELECT l FROM AuditLog l ORDER BY l.createdAt DESC")
    List<AuditLog> findRecentLogs(Pageable pageable);

    // Recherche par utilisateur et période
    List<AuditLog> findByUserIdAndCreatedAtBetween(UUID userId,
                                                   LocalDateTime start,
                                                   LocalDateTime end);

    // Recherche par adresse IP
    List<AuditLog> findByIpAddress(String ipAddress);

    // Compter les actions par type
    @Query("SELECT l.action, COUNT(l) FROM AuditLog l " +
            "WHERE l.createdAt >= :since GROUP BY l.action")
    List<Object[]> countActionsSince(@Param("since") LocalDateTime since);

    // Recherche des logs avec filtres combinés
    @Query("SELECT l FROM AuditLog l WHERE " +
            "(:userId IS NULL OR l.userId = :userId) AND " +
            "(:action IS NULL OR l.action = :action) AND " +
            "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR l.createdAt <= :endDate)")
    Page<AuditLog> findLogsWithFilters(@Param("userId") UUID userId,
                                       @Param("action") String action,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate,
                                       Pageable pageable);

    // Supprimer les logs plus anciens qu'une date (pour RGPD)
    void deleteByCreatedAtBefore(LocalDateTime date);
}