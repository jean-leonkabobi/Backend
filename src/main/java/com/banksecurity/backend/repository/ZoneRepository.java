package com.banksecurity.backend.repository;

import com.banksecurity.backend.model.Zone;
import com.banksecurity.backend.model.enums.ZoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    // Recherche par type de zone
    List<Zone> findByType(ZoneType type);

    // Recherche par caméra
    List<Zone> findByCameraId(UUID cameraId);

    // Recherche des zones actives
    List<Zone> findByIsActiveTrue();

    // Recherche des zones par caméra et type
    List<Zone> findByCameraIdAndType(UUID cameraId, ZoneType type);

    // Recherche des zones avec leurs règles
    @Query("SELECT DISTINCT z FROM Zone z LEFT JOIN FETCH z.rules WHERE z.id = :id")
    Optional<Zone> findByIdWithRules(@Param("id") UUID id);

    // Recherche des zones avec leurs caméras
    @Query("SELECT DISTINCT z FROM Zone z LEFT JOIN FETCH z.camera WHERE z.id = :id")
    Optional<Zone> findByIdWithCamera(@Param("id") UUID id);

    // Compter les zones par type
    @Query("SELECT COUNT(z) FROM Zone z WHERE z.type = :type")
    long countByType(@Param("type") ZoneType type);

    // Recherche des zones par sensibilité minimale
    List<Zone> findBySensitivityGreaterThanEqual(Integer sensitivity);

    // Recherche des zones inactives
    List<Zone> findByIsActiveFalse();

    // Recherche paginée avec tri
    @Query("SELECT z FROM Zone z ORDER BY z.createdAt DESC")
    List<Zone> findAllOrderByCreatedAtDesc();

    // Vérifier si une zone existe dans une caméra
    boolean existsByCameraIdAndName(UUID cameraId, String name);
}