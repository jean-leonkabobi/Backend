package com.banksecurity.backend.service;

import com.banksecurity.backend.dto.request.LoginRequest;
import com.banksecurity.backend.dto.request.RegisterRequest;
import com.banksecurity.backend.dto.response.AuthResponse;

public interface AuthService {

    /**
     * Authentifie un utilisateur et génère les tokens JWT
     */
    AuthResponse login(LoginRequest loginRequest);

    /**
     * Enregistre un nouvel utilisateur
     */
    AuthResponse register(RegisterRequest registerRequest);

    /**
     * Rafraîchit le token d'accès avec un refresh token
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Déconnecte l'utilisateur (invalide le token)
     */
    void logout(String token);

    /**
     * Valide un token JWT
     */
    boolean validateToken(String token);

    /**
     * Récupère l'utilisateur courant
     */
    String getCurrentUser();
}