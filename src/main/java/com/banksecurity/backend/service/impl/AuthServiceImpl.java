package com.banksecurity.backend.service.impl;

import com.banksecurity.backend.dto.request.LoginRequest;
import com.banksecurity.backend.dto.request.RegisterRequest;
import com.banksecurity.backend.dto.response.AuthResponse;
import com.banksecurity.backend.exception.BadRequestException;
import com.banksecurity.backend.exception.ConflictException;
import com.banksecurity.backend.exception.UnauthorizedException;
import com.banksecurity.backend.model.User;
import com.banksecurity.backend.model.enums.UserRole;
import com.banksecurity.backend.repository.UserRepository;
import com.banksecurity.backend.security.JwtTokenProvider;
import com.banksecurity.backend.security.UserPrincipal;
import com.banksecurity.backend.service.AuthService;
import com.banksecurity.backend.service.AuditLogService;
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
            // Authentifier l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            // Générer les tokens
            String token = tokenProvider.generateToken(userPrincipal);
            String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

            // Mettre à jour la date de dernière connexion
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));
            user.setLastLogin(LocalDateTime.now());
            user.setFailedAttempts(0);
            userRepository.save(user);

            // Journaliser l'action
            auditLogService.logAction(user.getId(), "LOGIN", "Connexion réussie");

            log.info("Utilisateur connecté: {}", userPrincipal.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(tokenProvider.getTokenValidityInSeconds())
                    .userId(userPrincipal.getId())
                    .email(userPrincipal.getEmail())
                    .firstName(userPrincipal.getFirstName())
                    .lastName(userPrincipal.getLastName())
                    .role(userPrincipal.getRole())
                    .build();

        } catch (Exception e) {
            // Incrémenter le compteur d'échecs
            userRepository.findByEmail(loginRequest.getEmail()).ifPresent(user -> {
                user.setFailedAttempts(user.getFailedAttempts() + 1);
                if (user.getFailedAttempts() >= 5) {
                    user.setAccountLocked(true);
                    log.warn("Compte verrouillé après 5 tentatives: {}", user.getEmail());
                }
                userRepository.save(user);
            });

            log.error("Échec de connexion pour {}: {}", loginRequest.getEmail(), e.getMessage());
            throw new UnauthorizedException("Email ou mot de passe incorrect");
        }
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // ✅ Utilisation de ConflictException pour les doublons
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new ConflictException("Cet email est déjà utilisé: " + registerRequest.getEmail());
        }

        // Créer le nouvel utilisateur
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

        // Générer les tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String token = tokenProvider.generateToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        // Journaliser l'action
        auditLogService.logAction(user.getId(), "REGISTER", "Nouvel utilisateur créé");

        log.info("Nouvel utilisateur enregistré: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Refresh token invalide");
        }

        // Extraire l'utilisateur du token
        String email = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Utilisateur non trouvé"));

        UserPrincipal userPrincipal = UserPrincipal.create(user);

        // Générer de nouveaux tokens
        String newToken = tokenProvider.generateToken(userPrincipal);
        String newRefreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getTokenValidityInSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }

    @Override
    public void logout(String token) {
        // Avec JWT, le logout est géré côté client (suppression du token)
        // Mais on peut journaliser l'action
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            auditLogService.logAction(userPrincipal.getId(), "LOGOUT", "Déconnexion");
            log.info("Utilisateur déconnecté: {}", userPrincipal.getEmail());
        }

        SecurityContextHolder.clearContext();
    }

    @Override
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    @Override
    public String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getUsername();
        }
        return null;
    }
}