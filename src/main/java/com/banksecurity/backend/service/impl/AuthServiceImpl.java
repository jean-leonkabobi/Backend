package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.LoginRequest;
import com.banksecurity.backend.dto.request.RegisterRequest;
import com.banksecurity.backend.dto.response.AuthResponse;
import com.banksecurity.backend.exception.ConflictException;
import com.banksecurity.backend.exception.ForbiddenException;
import com.banksecurity.backend.exception.ResourceNotFoundException;
import com.banksecurity.backend.exception.UnauthorizedException;
import com.banksecurity.backend.model.User;
import com.banksecurity.backend.model.enums.UserRole;
import com.banksecurity.backend.repository.UserRepository;
import com.banksecurity.backend.security.JwtTokenProvider;
import com.banksecurity.backend.security.UserPrincipal;
import com.banksecurity.backend.service.AuthService;
import com.banksecurity.backend.service.AuditLogService;
import com.banksecurity.backend.util.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            String token = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

            String extractedRole = tokenProvider.getRoleFromToken(token);
            log.debug("Rôle extrait du token: {}", extractedRole);

            long refreshValidity = tokenProvider.getRefreshTokenValidityInSeconds();
            log.debug("Durée de validité du refresh token: {} secondes", refreshValidity);

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "id", userPrincipal.getId()));
            user.setLastLogin(LocalDateTime.now());
            user.setFailedAttempts(0);
            userRepository.save(user);

            // ✅ Utilisation de Constants.AUDIT_ACTION_LOGIN
            auditLogService.logAction(user.getId(), Constants.AUDIT_ACTION_LOGIN, "Connexion réussie");

            log.info("Utilisateur connecté: {}", userPrincipal.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .tokenType(Constants.JWT_TYPE)
                    .expiresIn(tokenProvider.getTokenValidityInSeconds())
                    .userId(userPrincipal.getId())
                    .email(userPrincipal.getEmail())
                    .firstName(userPrincipal.getFirstName())
                    .lastName(userPrincipal.getLastName())
                    .role(userPrincipal.getRole())
                    .build();

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            userRepository.findByEmail(loginRequest.getEmail()).ifPresent(user -> {
                user.setFailedAttempts(user.getFailedAttempts() + 1);
                if (user.getFailedAttempts() >= 5) {
                    user.setAccountLocked(true);
                    log.warn("Compte verrouillé après 5 tentatives: {}", user.getEmail());
                }
                userRepository.save(user);
            });

            log.error("Échec de connexion pour {}: {}", loginRequest.getEmail(), e.getMessage());
            throw new UnauthorizedException("Email ou mot de passe incorrect", e);
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        try {
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                throw new ConflictException("Cet email est déjà utilisé: " + registerRequest.getEmail());
            }

            User user = User.builder()
                    .email(registerRequest.getEmail())
                    .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .role(registerRequest.getRole() != null ? registerRequest.getRole() : UserRole.SECURITY)
                    .phoneNumber(registerRequest.getPhoneNumber())
                    .isActive(true)
                    .accountLocked(false)
                    .failedAttempts(0)
                    .build();

            user = userRepository.save(user);

            UserPrincipal userPrincipal = UserPrincipal.create(user);
            String token = tokenProvider.generateToken(userPrincipal);
            String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

            auditLogService.logAction(user.getId(), Constants.AUDIT_ACTION_CREATE + "_USER", "Nouvel utilisateur créé");

            log.info("Nouvel utilisateur enregistré: {}", user.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .tokenType(Constants.JWT_TYPE)
                    .expiresIn(tokenProvider.getTokenValidityInSeconds())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole())
                    .build();

        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de {}: {}", registerRequest.getEmail(), e.getMessage(), e);
            throw new ConflictException("Erreur lors de l'enregistrement: " + registerRequest.getEmail(), e);
        }
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            if (tokenProvider.isTokenExpired(refreshToken)) {
                throw new UnauthorizedException("Refresh token expiré");
            }

            if (!tokenProvider.validateToken(refreshToken)) {
                throw new UnauthorizedException("Refresh token invalide");
            }

            String email = tokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));

            UserPrincipal userPrincipal = UserPrincipal.create(user);

            String newToken = tokenProvider.generateToken(userPrincipal);
            String newRefreshToken = tokenProvider.generateRefreshToken(userPrincipal);

            return AuthResponse.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .tokenType(Constants.JWT_TYPE)
                    .expiresIn(tokenProvider.getTokenValidityInSeconds())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole())
                    .build();

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du rafraîchissement du token: {}", e.getMessage(), e);
            throw new UnauthorizedException("Erreur lors du rafraîchissement du token", e);
        }
    }

    @Override
    public void logout(String token) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        // ✅ Utilisation de Constants.AUDIT_ACTION_LOGOUT
        auditLogService.logAction(userPrincipal.getId(), Constants.AUDIT_ACTION_LOGOUT, "Déconnexion");
        log.info("Utilisateur déconnecté: {}", userPrincipal.getEmail());

        SecurityContextHolder.clearContext();
    }

    @Override
    public boolean validateToken(String token) {
        if (tokenProvider.isTokenExpired(token)) {
            log.warn("Token expiré");
            return false;
        }
        return tokenProvider.validateToken(token);
    }

    @Override
    public String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new ForbiddenException("Utilisateur non authentifié");
        }
        return ((UserPrincipal) authentication.getPrincipal()).getUsername();
    }
}