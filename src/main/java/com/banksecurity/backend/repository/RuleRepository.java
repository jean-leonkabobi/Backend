package com.banksecurity.backend.repository;

import com.banksecurity.backend.model.Rule;
import com.banksecurity.backend.model.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {

    // Recherche par type de règle
    List<Rule> findByType(RuleType type);

    // Recherche par zone
    List<Rule> findByZoneId(UUID zoneId);

    // Recherche des règles actives
    List<Rule> findByIsActiveTrue();

    // Recherche des règles inactives
    List<Rule> findByIsActiveFalse();

    // Recherche des règles par zone et type
    List<Rule> findByZoneIdAndType(UUID zoneId, RuleType type);

    // Recherche des règles avec leur zone
    @Query("SELECT DISTINCT r FROM Rule r LEFT JOIN FETCH r.zone WHERE r.id = :id")
    Optional<Rule> findByIdWithZone(@Param("id") UUID id);

    // Recherche des règles par priorité minimale
    List<Rule> findByPriorityGreaterThanEqual(Integer priority);

    // Compter les règles actives par type
    @Query("SELECT COUNT(r) FROM Rule r WHERE r.type = :type AND r.isActive = true")
    long countActiveByType(@Param("type") RuleType type);

    // Recherche des règles avec un seuil de temps spécifique
    List<Rule> findByThresholdTimeGreaterThan(Integer thresholdTime);

    // Recherche paginée avec tri
    @Query("SELECT r FROM Rule r ORDER BY r.priority DESC, r.createdAt DESC")
    List<Rule> findAllOrderByPriorityDesc();

    // Recherche des règles actives avec leur zone
    @Query("SELECT DISTINCT r FROM Rule r LEFT JOIN FETCH r.zone WHERE r.isActive = true")
    List<Rule> findActiveRulesWithZones();
}