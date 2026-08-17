package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.UserRequest;
import com.banksecurity.backend.dto.response.UserResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ConflictException;
import com.banksecurity.backend.exception.ForbiddenException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.model.User;
import com.banksecurity.backend.model.enums.UserRole;
import com.banksecurity.backend.repository.UserRepository;
import com.banksecurity.backend.security.UserPrincipal;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.service.UserService;
import com.banksecurity.backend.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    // ==================== MÉTHODES CRUD EXISTANTES ====================

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        try {
            // ✅ ValidationUtils.isNullOrEmpty
            if (ValidationUtils.isNullOrEmpty(request.getEmail())) {
                throw new BadRequestException("L'email est obligatoire");
            }

            // ✅ ValidationUtils.isInvalidEmail (méthode négative)
            if (ValidationUtils.isInvalidEmail(request.getEmail())) {
                throw new BadRequestException("Format d'email invalide: " + request.getEmail());
            }

            // ✅ ValidationUtils.isWeakPassword (méthode négative)
            if (ValidationUtils.isWeakPassword(request.getPassword())) {
                throw new BadRequestException("Mot de passe trop faible (min 8 caractères, majuscule, minuscule, chiffre, caractère spécial)");
            }

            // ✅ ValidationUtils.isInvalidPhone (méthode négative)
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty() &&
                    ValidationUtils.isInvalidPhone(request.getPhoneNumber())) {
                throw new BadRequestException("Format de téléphone invalide: " + request.getPhoneNumber());
            }

            // ✅ ValidationUtils.sanitizeString
            String cleanedEmail = ValidationUtils.sanitizeString(request.getEmail());
            String cleanedFirstName = ValidationUtils.sanitizeString(request.getFirstName());
            String cleanedLastName = ValidationUtils.sanitizeString(request.getLastName());

            if (userRepository.existsByEmail(cleanedEmail)) {
                throw new ConflictException("Cet email est déjà utilisé: " + cleanedEmail);
            }

            User user = User.builder()
                    .email(cleanedEmail)
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .firstName(cleanedFirstName)
                    .lastName(cleanedLastName)
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

        } catch (ConflictException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la création de l'utilisateur: {}", e.getMessage(), e);
            throw new ConflictException("Erreur lors de la création de l'utilisateur: " + request.getEmail(), e);
        }
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

            // ✅ ValidationUtils.isInvalidEmail (méthode négative)
            if (ValidationUtils.isInvalidEmail(request.getEmail())) {
                throw new BadRequestException("Format d'email invalide: " + request.getEmail());
            }

            // ✅ ValidationUtils.sanitizeString
            String cleanedEmail = ValidationUtils.sanitizeString(request.getEmail());
            String cleanedFirstName = ValidationUtils.sanitizeString(request.getFirstName());
            String cleanedLastName = ValidationUtils.sanitizeString(request.getLastName());

            if (!user.getEmail().equals(cleanedEmail) &&
                    userRepository.existsByEmail(cleanedEmail)) {
                throw new ConflictException("Cet email est déjà utilisé: " + cleanedEmail);
            }

            user.setEmail(cleanedEmail);
            user.setFirstName(cleanedFirstName);
            user.setLastName(cleanedLastName);

            if (request.getRole() != null) {
                user.setRole(request.getRole());
            }

            if (request.getPhoneNumber() != null) {
                // ✅ ValidationUtils.isInvalidPhone (méthode négative)
                if (ValidationUtils.isInvalidPhone(request.getPhoneNumber())) {
                    throw new BadRequestException("Format de téléphone invalide: " + request.getPhoneNumber());
                }
                user.setPhoneNumber(request.getPhoneNumber());
            }

            if (request.getIsActive() != null) {
                user.setIsActive(request.getIsActive());
            }

            if (request.getPassword() != null && !request.getPassword().isEmpty()) {
                // ✅ ValidationUtils.isWeakPassword (méthode négative)
                if (ValidationUtils.isWeakPassword(request.getPassword())) {
                    throw new BadRequestException("Mot de passe trop faible");
                }
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            }

            user = userRepository.save(user);

            auditLogService.logAction(null, "UPDATE_USER", "Mise à jour utilisateur: " + user.getEmail());
            log.info("Utilisateur mis à jour: {}", user.getEmail());

            return mapToResponse(user);

        } catch (ConflictException e) {
            throw e;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour de l'utilisateur: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la mise à jour de l'utilisateur: " + id, e);
        }
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ForbiddenException("Vous n'avez pas les permissions pour supprimer un utilisateur");
        }

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

            userRepository.delete(user);

            auditLogService.logAction(null, "DELETE_USER", "Suppression utilisateur: " + user.getEmail());
            log.info("Utilisateur supprimé: {}", user.getEmail());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'utilisateur: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la suppression de l'utilisateur: " + id, e);
        }
    }

    @Override
    public UserResponse getUserById(UUID id) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));
            return mapToResponse(user);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'utilisateur: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la récupération de l'utilisateur: " + id, e);
        }
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
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

            user.setIsActive(isActive);
            user = userRepository.save(user);

            auditLogService.logAction(null, "TOGGLE_USER_STATUS",
                    "Statut utilisateur " + user.getEmail() + " -> " + (isActive ? "actif" : "inactif"));

            return mapToResponse(user);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du changement de statut: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors du changement de statut: " + id, e);
        }
    }

    @Override
    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ForbiddenException("Vous n'avez pas les permissions pour réinitialiser ce mot de passe");
        }

        try {
            // ✅ ValidationUtils.isWeakPassword (méthode négative)
            if (ValidationUtils.isWeakPassword(newPassword)) {
                throw new BadRequestException("Mot de passe trop faible (min 8 caractères, majuscule, minuscule, chiffre, caractère spécial)");
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setPasswordResetToken(null);
            user.setPasswordResetExpiry(null);
            userRepository.save(user);

            auditLogService.logAction(null, "RESET_PASSWORD", "Réinitialisation mot de passe: " + user.getEmail());
            log.info("Mot de passe réinitialisé pour: {}", user.getEmail());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la réinitialisation du mot de passe: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors de la réinitialisation du mot de passe: " + id, e);
        }
    }

    @Override
    public List<UserResponse> searchUsers(String searchTerm) {
        // ✅ ValidationUtils.sanitizeString
        String cleanedTerm = ValidationUtils.sanitizeString(searchTerm);
        return userRepository.searchUsers(cleanedTerm).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleAccountLock(UUID id, boolean locked) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", id));

            user.setAccountLocked(locked);
            if (!locked) {
                user.setFailedAttempts(0);
            }
            userRepository.save(user);

            auditLogService.logAction(null, "TOGGLE_ACCOUNT_LOCK",
                    "Compte " + user.getEmail() + " -> " + (locked ? "verrouillé" : "déverrouillé"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du verrouillage/déverrouillage: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("Erreur lors du verrouillage/déverrouillage: " + id, e);
        }
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public long countUsersByRole(UserRole role) {
        return userRepository.countByRole(role);
    }

    // ==================== NOUVELLES MÉTHODES UTILISANT LES REPOSITORY ====================

    public List<UserResponse> getActiveUsers() {
        return userRepository.findByIsActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getInactiveUsers() {
        return userRepository.findByIsActiveFalse().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getLockedAccounts() {
        return userRepository.findByAccountLockedTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getInactiveUsersSince(int days) {
        LocalDateTime date = LocalDateTime.now().minusDays(days);
        return userRepository.findInactiveUsers(date).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserByPasswordResetToken(String token) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "passwordResetToken", token));
        return mapToResponse(user);
    }

    public List<UserResponse> getUsersWithFailedAttempts(int attempts) {
        return userRepository.findUsersWithFailedAttempts(attempts).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getAllUsersOrderedByCreation() {
        return userRepository.findAllOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
        return mapToResponse(user);
    }

    public long countActiveUsers() {
        return userRepository.findByIsActiveTrue().size();
    }

    public long countInactiveUsers() {
        return userRepository.findByIsActiveFalse().size();
    }

    public long countLockedAccounts() {
        return userRepository.findByAccountLockedTrue().size();
    }

    // ==================== MÉTHODE DE MAPPING ====================

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