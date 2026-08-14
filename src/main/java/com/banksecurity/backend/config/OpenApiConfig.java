package com.banksecurity.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Configuration OpenAPI/Swagger pour la documentation de l'API
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankSecurityOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("API Agent de Sécurité Bancaire")
                        .description("""
                                API REST pour le système de sécurité intelligent des agences bancaires.
                                
                                ## Fonctionnalités principales:
                                * **Gestion des caméras** - Configuration et surveillance des caméras IP
                                * **Zones virtuelles** - Définition de zones sensibles (DAB, back-office, etc.)
                                * **Règles de détection** - Configuration des règles d'alerte
                                * **Alertes** - Gestion des alertes de sécurité en temps réel
                                * **Statistiques** - Tableau de bord et rapports
                                
                                ## Niveaux d'accès:
                                * **ADMIN** - Accès complet
                                * **MANAGER** - Gestion des caméras, zones et règles
                                * **SECURITY** - Consultation des alertes et statistiques
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe de développement")
                                .email("dev@banksecurity.com")
                                .url("https://banksecurity.com"))
                        .license(new License()
                                .name("Propriétaire")
                                .url("https://banksecurity.com/license")))

                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Serveur de développement"))

                .addServersItem(new Server()
                        .url("https://api.banksecurity.com")
                        .description("Serveur de production"))

                // Configuration de la sécurité JWT
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Entrez votre token JWT")))

                // Tags pour organiser la documentation
                .tags(Arrays.asList(
                        new Tag().name("Authentification")
                                .description("Endpoints d'authentification et de gestion des comptes"),
                        new Tag().name("Caméras")
                                .description("Gestion des caméras IP et de leur configuration"),
                        new Tag().name("Zones")
                                .description("Définition des zones virtuelles de surveillance"),
                        new Tag().name("Règles")
                                .description("Configuration des règles de détection"),
                        new Tag().name("Alertes")
                                .description("Gestion des alertes de sécurité"),
                        new Tag().name("Utilisateurs")
                                .description("Gestion des utilisateurs et des rôles"),
                        new Tag().name("Tableau de bord")
                                .description("Statistiques et indicateurs de sécurité"),
                        new Tag().name("Administration")
                                .description("Endpoints d'administration système")
                ));
    }
}