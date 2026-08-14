package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.request.UserRequest;
import com.banksecurity.backend.dto.response.UserResponse;
import com.banksecurity.backend.model.enums.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserService {

    /**
     * Crée un nouvel utilisateur
     */
    UserResponse createUser(UserRequest request);

    /**
     * Met à jour un utilisateur existant
     */
    UserResponse updateUser(UUID id, UserRequest request);

    /**
     * Supprime un utilisateur
     */
    void deleteUser(UUID id);

    /**
     * Récupère un utilisateur par son ID
     */
    UserResponse getUserById(UUID id);

    /**
     * Récupère tous les utilisateurs
     */
    List<UserResponse> getAllUsers();

    /**
     * Récupère les utilisateurs par rôle
     */
    List<UserResponse> getUsersByRole(UserRole role);

    /**
     * Active ou désactive un utilisateur
     */
    UserResponse toggleUserStatus(UUID id, boolean isActive);

    /**
     * Réinitialise le mot de passe d'un utilisateur
     */
    void resetPassword(UUID id, String newPassword);

    /**
     * Recherche des utilisateurs par terme
     */
    List<UserResponse> searchUsers(String searchTerm);

    /**
     * Verrouille ou déverrouille un compte utilisateur
     */
    void toggleAccountLock(UUID id, boolean locked);

    /**
     * Récupère le nombre total d'utilisateurs
     */
    long countUsers();

    /**
     * Récupère le nombre d'utilisateurs par rôle
     */
    long countUsersByRole(UserRole role);
}