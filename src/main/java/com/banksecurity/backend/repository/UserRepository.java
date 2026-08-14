package com.banksecurity.backend.repository;

import com.banksecurity.backend.model.User;
import com.banksecurity.backend.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Recherche par email (pour l'authentification)
    Optional<User> findByEmail(String email);

    // Vérifier si un email existe déjà
    boolean existsByEmail(String email);

    // Recherche par rôle
    List<User> findByRole(UserRole role);

    // Recherche des utilisateurs actifs
    List<User> findByIsActiveTrue();

    // Recherche des utilisateurs inactifs
    List<User> findByIsActiveFalse();

    // Recherche des comptes verrouillés
    List<User> findByAccountLockedTrue();

    // Recherche par nom ou prénom (recherche partielle)
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    // Recherche des utilisateurs qui ne se sont pas connectés depuis X jours
    @Query("SELECT u FROM User u WHERE u.lastLogin < :date OR u.lastLogin IS NULL")
    List<User> findInactiveUsers(@Param("date") LocalDateTime date);

    // Compter les utilisateurs par rôle
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRole role);

    // Recherche par token de réinitialisation de mot de passe
    Optional<User> findByPasswordResetToken(String token);

    // Recherche des utilisateurs avec des tentatives de connexion échouées
    @Query("SELECT u FROM User u WHERE u.failedAttempts >= :attempts")
    List<User> findUsersWithFailedAttempts(@Param("attempts") int attempts);

    // Recherche paginée avec tri
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findAllOrderByCreatedAtDesc();
}