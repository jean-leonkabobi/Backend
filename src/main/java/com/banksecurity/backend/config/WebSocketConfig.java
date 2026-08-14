package com.banksecurity.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration WebSocket pour la communication en temps réel
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure les endpoints STOMP pour la communication WebSocket
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Préfixe pour les messages envoyés au client
        registry.enableSimpleBroker(
                "/topic",    // Diffusion générale (tous les clients)
                "/queue"     // Messages spécifiques à un utilisateur
        );

        // Préfixe pour les messages envoyés par le client au serveur
        registry.setApplicationDestinationPrefixes("/app");

        // Configuration du broker pour les messages utilisateur
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Enregistre les endpoints STOMP
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://192.168.1.100:3000"
                )
                .withSockJS(); // Fallback SockJS pour les navigateurs sans WebSocket

        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://192.168.1.100:3000"
                );

        log.info("Endpoints WebSocket enregistrés sur /ws");
    }

    /**
     * Configure le canal de messages entrant
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketAuthInterceptor());
    }

    /**
     * Configure le canal de messages sortant
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Configuration supplémentaire si nécessaire
    }

    /**
     * Intercepteur d'authentification pour les connexions WebSocket
     */
    private static class WebSocketAuthInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            // Récupérer les headers du message
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            StompCommand command = accessor.getCommand();

            // Vérifier l'authentification pour les commandes CONNECT
            if (StompCommand.CONNECT.equals(command)) {
                String authToken = accessor.getFirstNativeHeader("Authorization");

                if (authToken == null || authToken.isEmpty()) {
                    log.warn("Tentative de connexion WebSocket sans token");
                    throw new IllegalArgumentException(
                            "Authentification requise pour la connexion WebSocket"
                    );
                }

                // TODO: Valider le token JWT ici
                log.debug("Connexion WebSocket avec token: {}",
                        authToken.substring(0, Math.min(10, authToken.length())) + "...");
            }

            return message;
        }
    }
}