package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.UserRequest;
import com.banksecurity.backend.dto.response.UserResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.User;
import com.banksecurity.backend.model.enums.UserRole;
import com.banksecurity.backend.repository.UserRepository;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Le mot de passe est obligatoire");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : UserRole.SECURITY)
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .accountLocked(false)
                .failedAttempts(0)
                .build();

        user = userRepository.save(user);

        auditLogService.logAction(null, "CREATE_USER", "Création utilisateur: " + user.getEmail());
        log.info("Utilisateur créé: {}", user.getEmail());

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        // Vérifier si le nouvel email n'est pas déjà utilisé
        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
        }

        // Mettre à jour le mot de passe si fourni
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);

        auditLogService.logAction(null, "UPDATE_USER", "Mise à jour utilisateur: " + user.getEmail());
        log.info("Utilisateur mis à jour: {}", user.getEmail());

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        userRepository.delete(user);

        auditLogService.logAction(null, "DELETE_USER", "Suppression utilisateur: " + user.getEmail());
        log.info("Utilisateur supprimé: {}", user.getEmail());
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
        return mapToResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(UUID id, boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        user.setIsActive(isActive);
        user = userRepository.save(user);

        auditLogService.logAction(null, "TOGGLE_USER_STATUS",
                "Statut utilisateur " + user.getEmail() + " -> " + (isActive ? "actif" : "inactif"));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        auditLogService.logAction(null, "RESET_PASSWORD", "Réinitialisation mot de passe: " + user.getEmail());
        log.info("Mot de passe réinitialisé pour: {}", user.getEmail());
    }

    @Override
    public List<UserResponse> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleAccountLock(UUID id, boolean locked) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

        user.setAccountLocked(locked);
        if (!locked) {
            user.setFailedAttempts(0);
        }
        userRepository.save(user);

        auditLogService.logAction(null, "TOGGLE_ACCOUNT_LOCK",
                "Compte " + user.getEmail() + " -> " + (locked ? "verrouillé" : "déverrouillé"));
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}