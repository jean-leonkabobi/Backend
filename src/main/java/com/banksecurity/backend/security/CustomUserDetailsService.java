package com.banksecurity.backend.security;

import com.banksecurity.backend.model.User;
import com.banksecurity.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
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
     */
    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(java.util.UUID.fromString(id.toString()))
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'ID: " + id));

        return UserPrincipal.create(user);
    }
}