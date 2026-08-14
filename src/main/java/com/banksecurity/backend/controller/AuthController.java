package com.banksecurity.backend.controller;

import com.banksecurity.backend.dto.request.LoginRequest;
import com.banksecurity.backend.dto.request.RegisterRequest;
import com.banksecurity.backend.dto.response.ApiResponse;
import com.banksecurity.backend.dto.response.AuthResponse;
import com.banksecurity.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints d'authentification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authentifier un utilisateur", description = "Retourne les tokens JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Tentative de connexion pour: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Connexion réussie", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Enregistrer un nouvel utilisateur")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Tentative d'enregistrement pour: {}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Enregistrement réussi", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraîchir le token JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody String refreshToken) {
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token rafraîchi", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Déconnecter l'utilisateur")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Déconnexion réussie", null));
    }

    @GetMapping("/validate")
    @Operation(summary = "Valider un token JWT")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        boolean isValid = authService.validateToken(jwt);
        return ResponseEntity.ok(ApiResponse.success("Validation effectuée", isValid));
    }
}