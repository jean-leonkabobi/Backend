package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.request.UserRequest;
import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.dto.response.UserResponse;
import com.banksecurity.backend.model.enums.UserRole;
import com.banksecurity.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Récupérer tous les utilisateurs")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un utilisateur par ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur récupéré", user));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Récupérer les utilisateurs par rôle")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsersByRole(@PathVariable UserRole role) {
        List<UserResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", users));
    }

    @GetMapping("/search")
    @Operation(summary = "Rechercher des utilisateurs")
    public ResponseEntity<ApiResponse<List<UserResponse>>> searchUsers(@RequestParam String term) {
        List<UserResponse> users = userService.searchUsers(term);
        return ResponseEntity.ok(ApiResponse.success("Résultats de recherche", users));
    }

    @PostMapping
    @Operation(summary = "Créer un nouvel utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utilisateur créé", user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable UUID id,
                                                                @Valid @RequestBody UserRequest request) {
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur mis à jour", user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé", null));
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Activer/désactiver un utilisateur")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable UUID id,
                                                                      @RequestParam boolean isActive) {
        UserResponse user = userService.toggleUserStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("Statut mis à jour", user));
    }

    @PutMapping("/{id}/toggle-lock")
    @Operation(summary = "Verrouiller/déverrouiller un compte")
    public ResponseEntity<ApiResponse<Void>> toggleAccountLock(@PathVariable UUID id,
                                                               @RequestParam boolean locked) {
        userService.toggleAccountLock(id, locked);
        return ResponseEntity.ok(ApiResponse.success("Compte mis à jour", null));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable UUID id,
                                                           @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe réinitialisé", null));
    }
}