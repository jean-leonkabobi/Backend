package com.banksecurity.backend.security;

import com.banksecurity.backend.model.User;
import com.banksecurity.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    @Nullable
    public UserDetails loadUserByUsername(@Nullable String email) throws UsernameNotFoundException {
        if (email == null || email.isEmpty()) {
            throw new UsernameNotFoundException("Email non fourni");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'email: " + email));

        // Vérifier si le compte est verrouillé
        if (user.getAccountLocked()) {
            log.warn("Tentative de connexion sur un compte verrouillé: {}", email);
            throw new UsernameNotFoundException("Compte verrouillé. Contactez l'administrateur.");
        }

        // Vérifier si le compte est actif
        if (!user.getIsActive()) {
            log.warn("Tentative de connexion sur un compte inactif: {}", email);
            throw new UsernameNotFoundException("Compte inactif. Contactez l'administrateur.");
        }

        // Mettre à jour la date de dernière connexion
        user.setLastLogin(LocalDateTime.now());
        user.setFailedAttempts(0);
        userRepository.save(user);

        return UserPrincipal.create(user);
    }

    /**
     * Charge un utilisateur par son ID
     * Utilisé par JwtAuthenticationFilter pour valider le token JWT
     */
    @Transactional
    @SuppressWarnings("unused")
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        return UserPrincipal.create(user);
    }
}