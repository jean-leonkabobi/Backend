package com.banksecurity.backend.config;

import com.banksecurity.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Constantes pour les rôles
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_SECURITY = "SECURITY";

    // Constantes pour les messages d'erreur
    private static final String JSON_UNAUTHORIZED = "{\"status\":401,\"error\":\"Non autorisé\",\"message\":\"Authentification requise\"}";
    private static final String JSON_FORBIDDEN = "{\"status\":403,\"error\":\"Accès refusé\",\"message\":\"Permissions insuffisantes\"}";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Configuration de la chaîne de filtres de sécurité
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // Désactiver CSRF car nous utilisons JWT
                .csrf(csrf -> csrf.disable())

                // Activer CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configuration des sessions (stateless pour JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configuration des autorisations
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Endpoints d'administration
                        .requestMatchers("/users/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/cameras/**").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)
                        .requestMatchers("/zones/**").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)
                        .requestMatchers("/rules/**").hasAnyRole(ROLE_ADMIN, ROLE_MANAGER)

                        // Endpoints de sécurité
                        .requestMatchers("/alerts/**").hasAnyRole(ROLE_ADMIN, ROLE_SECURITY, ROLE_MANAGER)
                        .requestMatchers("/dashboard/**").hasAnyRole(ROLE_ADMIN, ROLE_SECURITY, ROLE_MANAGER)

                        // Tous les autres endpoints nécessitent une authentification
                        .anyRequest().authenticated()
                )

                // Ajouter le filtre JWT avant le filtre d'authentification
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Configuration de la gestion des exceptions
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::handleAuthenticationEntryPoint)
                        .accessDeniedHandler(this::handleAccessDeniedHandler)
                );

        return http.build();
    }

    /**
     * Gère les erreurs d'authentification (401)
     * Les paramètres sont conservés pour la signature de l'interface AuthenticationEntryPoint
     */
    private void handleAuthenticationEntryPoint(jakarta.servlet.http.HttpServletRequest request,
                                                HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(JSON_UNAUTHORIZED);
    }

    /**
     * Gère les erreurs d'accès refusé (403)
     * Les paramètres sont conservés pour la signature de l'interface AccessDeniedHandler
     */
    private void handleAccessDeniedHandler(jakarta.servlet.http.HttpServletRequest request,
                                           HttpServletResponse response,
                                           org.springframework.security.access.AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(JSON_FORBIDDEN);
    }

    /**
     * Encodeur de mots de passe (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Gestionnaire d'authentification
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configuration CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Autoriser les origines spécifiques (frontend React)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://192.168.1.100:3000"
        ));

        // Autoriser toutes les méthodes HTTP
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Autoriser les headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Exposer les headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "X-Total-Count"
        ));

        // Autoriser les credentials
        configuration.setAllowCredentials(true);

        // Durée de mise en cache des réponses CORS
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}