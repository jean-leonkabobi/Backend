package com.banksecurity.backend.repository;

import com.banksecurity.backend.model.Camera;
import com.banksecurity.backend.model.enums.CameraStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CameraRepository extends JpaRepository<Camera, UUID> {

    // Recherche par statut
    List<Camera> findByStatus(CameraStatus status);

    // Recherche des caméras actives
    List<Camera> findByStatusAndIsAnalyzingTrue(CameraStatus status);

    // Recherche par localisation (partielle)
    List<Camera> findByLocationContainingIgnoreCase(String location);

    // Recherche par adresse IP
    Optional<Camera> findByIpAddress(String ipAddress);

    // Recherche des caméras en erreur
    @Query("SELECT c FROM Camera c WHERE c.status = 'ERROR'")
    List<Camera> findCamerasInError();

    // Recherche des caméras qui n'ont pas envoyé de heartbeat depuis X minutes
    @Query("SELECT c FROM Camera c WHERE c.lastHeartbeat < :date OR c.lastHeartbeat IS NULL")
    List<Camera> findCamerasNotResponding(@Param("date") LocalDateTime date);

    // Compter les caméras par statut
    @Query("SELECT COUNT(c) FROM Camera c WHERE c.status = :status")
    long countByStatus(@Param("status") CameraStatus status);

    // Recherche des caméras avec leurs zones
    @Query("SELECT DISTINCT c FROM Camera c LEFT JOIN FETCH c.zones WHERE c.id = :id")
    Optional<Camera> findByIdWithZones(@Param("id") UUID id);

    // Recherche des caméras en enregistrement
    List<Camera> findByIsRecordingTrue();

    // Recherche des caméras en analyse
    List<Camera> findByIsAnalyzingTrue();

    // Recherche par modèle de caméra
    List<Camera> findByModelContainingIgnoreCase(String model);

    // Recherche paginée avec tri
    @Query("SELECT c FROM Camera c ORDER BY c.createdAt DESC")
    List<Camera> findAllOrderByCreatedAtDesc();
}