package com.banksecurity.backend.config;

import com.banksecurity.backend.security.JwtAuthenticationFilter;
import com.banksecurity.backend.util.Constants;
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

    // Constantes pour les messages d'erreur
    private static final String JSON_UNAUTHORIZED = "{\"status\":401,\"error\":\"Non autorisé\",\"message\":\"Authentification requise\"}";
    private static final String JSON_FORBIDDEN = "{\"status\":403,\"error\":\"Accès refusé\",\"message\":\"Permissions insuffisantes\"}";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints publics
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers(Constants.WS_ENDPOINT + "/**").permitAll()

                        // Endpoints d'administration - ✅ Utilisation des constantes
                        .requestMatchers("/users/**").hasRole(Constants.ROLE_ADMIN)
                        .requestMatchers("/cameras/**").hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)
                        .requestMatchers("/zones/**").hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)
                        .requestMatchers("/rules/**").hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_MANAGER)

                        // Endpoints de sécurité - ✅ Utilisation des constantes
                        .requestMatchers("/alerts/**").hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_SECURITY, Constants.ROLE_MANAGER)
                        .requestMatchers("/dashboard/**").hasAnyRole(Constants.ROLE_ADMIN, Constants.ROLE_SECURITY, Constants.ROLE_MANAGER)

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::handleAuthenticationEntryPoint)
                        .accessDeniedHandler(this::handleAccessDeniedHandler)
                );

        return http.build();
    }

    private void handleAuthenticationEntryPoint(jakarta.servlet.http.HttpServletRequest request,
                                                HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(JSON_UNAUTHORIZED);
    }

    private void handleAccessDeniedHandler(jakarta.servlet.http.HttpServletRequest request,
                                           HttpServletResponse response,
                                           org.springframework.security.access.AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(JSON_FORBIDDEN);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://192.168.1.100:3000"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ✅ Utilisation de Constants.JWT_HEADER
        configuration.setAllowedHeaders(Arrays.asList(
                Constants.JWT_HEADER,
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                Constants.JWT_HEADER,
                "Content-Disposition",
                "X-Total-Count"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}