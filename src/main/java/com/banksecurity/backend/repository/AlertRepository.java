package com.banksecurity.backend.repository;

import com.banksecurity.backend.model.Alert;
import com.banksecurity.backend.model.enums.AlertSeverity;
import com.banksecurity.backend.model.enums.AlertStatus;
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
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    // Recherche par statut
    List<Alert> findByStatus(AlertStatus status);

    // Recherche par sévérité
    List<Alert> findBySeverity(AlertSeverity severity);

    // Recherche par caméra
    List<Alert> findByCameraId(UUID cameraId);

    // Recherche par zone
    List<Alert> findByZoneId(UUID zoneId);

    // Recherche par règle
    List<Alert> findByRuleId(UUID ruleId);

    // Recherche par type
    List<Alert> findByType(String type);

    // Recherche paginée
    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);
    Page<Alert> findBySeverity(AlertSeverity severity, Pageable pageable);
    Page<Alert> findByCameraId(UUID cameraId, Pageable pageable);

    // Recherche par période
    List<Alert> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Recherche des alertes critiques non résolues
    @Query("SELECT a FROM Alert a WHERE a.severity = 'CRITICAL' AND a.status NOT IN ('RESOLVED', 'FALSE_ALARM')")
    List<Alert> findUnresolvedCriticalAlerts();

    // Recherche des alertes par sévérité et statut
    List<Alert> findBySeverityAndStatus(AlertSeverity severity, AlertStatus status);

    // Compter les alertes depuis une date
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.createdAt >= :since")
    long countAlertsSince(@Param("since") LocalDateTime since);

    // Compter les alertes par statut depuis une date
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status = :status AND a.createdAt >= :since")
    long countAlertsByStatusSince(@Param("status") AlertStatus status,
                                  @Param("since") LocalDateTime since);

    // Compter les alertes par sévérité depuis une date
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.severity = :severity AND a.createdAt >= :since")
    long countAlertsBySeveritySince(@Param("severity") AlertSeverity severity,
                                    @Param("since") LocalDateTime since);

    // Statistiques groupées par heure
    @Query("SELECT HOUR(a.createdAt) as hour, COUNT(a) as count " +
            "FROM Alert a WHERE a.createdAt >= :since " +
            "GROUP BY HOUR(a.createdAt) ORDER BY hour")
    List<Object[]> countAlertsByHourSince(@Param("since") LocalDateTime since);

    // Statistiques groupées par type
    @Query("SELECT a.type, COUNT(a) as count " +
            "FROM Alert a WHERE a.createdAt >= :since " +
            "GROUP BY a.type ORDER BY count DESC")
    List<Object[]> countAlertsByTypeSince(@Param("since") LocalDateTime since);

    // Statistiques groupées par caméra
    @Query("SELECT c.name, COUNT(a) as count " +
            "FROM Alert a JOIN a.camera c WHERE a.createdAt >= :since " +
            "GROUP BY c.name ORDER BY count DESC")
    List<Object[]> countAlertsByCameraSince(@Param("since") LocalDateTime since);

    // Recherche des alertes avec détails complets
    @Query("SELECT DISTINCT a FROM Alert a " +
            "LEFT JOIN FETCH a.camera " +
            "LEFT JOIN FETCH a.zone " +
            "LEFT JOIN FETCH a.rule " +
            "WHERE a.id = :id")
    Alert findAlertWithDetails(@Param("id") UUID id);

    // Recherche des alertes récentes non traitées
    @Query("SELECT a FROM Alert a WHERE a.status IN ('PENDING', 'ESCALATED') " +
            "AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentUnprocessedAlerts(@Param("since") LocalDateTime since);

    // Recherche paginée avec filtres combinés
    @Query("SELECT a FROM Alert a WHERE " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:severity IS NULL OR a.severity = :severity) AND " +
            "(:cameraId IS NULL OR a.camera.id = :cameraId) AND " +
            "(:startDate IS NULL OR a.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR a.createdAt <= :endDate)")
    Page<Alert> findAlertsWithFilters(@Param("status") AlertStatus status,
                                      @Param("severity") AlertSeverity severity,
                                      @Param("cameraId") UUID cameraId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);
}