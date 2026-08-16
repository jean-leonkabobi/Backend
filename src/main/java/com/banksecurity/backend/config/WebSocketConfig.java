package com.banksecurity.backend.config;

import com.banksecurity.backend.security.JwtTokenProvider;
import com.banksecurity.backend.util.Constants;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(
                "/topic",
                "/queue"
        );
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ✅ Utilisation de Constants.WS_ENDPOINT
        registry.addEndpoint(Constants.WS_ENDPOINT)
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://192.168.1.100:3000"
                )
                .withSockJS();

        registry.addEndpoint(Constants.WS_ENDPOINT)
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://192.168.1.100:3000"
                );

        log.info("Endpoints WebSocket enregistrés sur {}", Constants.WS_ENDPOINT);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketAuthInterceptor());
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Configuration supplémentaire si nécessaire
    }

    private class WebSocketAuthInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(@Nullable Message<?> message, @Nullable MessageChannel channel) {
            if (message == null || channel == null) {
                log.warn("Message ou canal WebSocket null");
                return null;
            }

            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            StompCommand command = accessor.getCommand();

            if (StompCommand.CONNECT.equals(command)) {
                // ✅ Utilisation de Constants.JWT_HEADER
                String authToken = accessor.getFirstNativeHeader(Constants.JWT_HEADER);

                if (authToken == null || authToken.isEmpty()) {
                    log.warn("Tentative de connexion WebSocket sans token");
                    throw new IllegalArgumentException("Authentification requise pour la connexion WebSocket");
                }

                // ✅ Utilisation de Constants.JWT_PREFIX
                String token = authToken.startsWith(Constants.JWT_PREFIX)
                        ? authToken.substring(Constants.JWT_PREFIX.length())
                        : authToken;

                if (!jwtTokenProvider.validateToken(token)) {
                    log.warn("Token JWT invalide pour la connexion WebSocket");
                    throw new IllegalArgumentException("Token JWT invalide");
                }

                log.debug("Connexion WebSocket authentifiée avec token valide");
            }

            return message;
        }
    }
}