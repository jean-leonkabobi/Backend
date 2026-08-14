package com.banksecurity.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration CORS détaillée pour le frontend React
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // Autoriser les origines du frontend
        corsConfiguration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",           // React dev server
                "http://localhost:5173",           // Vite dev server
                "http://192.168.1.100:3000",       // Réseau local
                "https://app.banksecurity.com"     // Production
        ));

        // Méthodes HTTP autorisées
        corsConfiguration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        // Headers autorisés
        corsConfiguration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Cache-Control",
                "Pragma",
                "Expires"
        ));

        // Headers exposés au frontend
        corsConfiguration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition",
                "X-Total-Count",
                "X-Error-Message"
        ));

        // Autoriser les cookies et credentials
        corsConfiguration.setAllowCredentials(true);

        // Durée de mise en cache des réponses preflight (1 heure)
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Appliquer cette configuration à toutes les routes
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsFilter(source);
    }
}