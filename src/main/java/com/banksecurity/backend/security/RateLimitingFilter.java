package com.banksecurity.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtre simple de limitation de débit pour prévenir les attaques par force brute
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_SIZE_MS = 60000; // 1 minute

    private final Map<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Ne pas limiter les endpoints publics sauf /auth/login
        String path = request.getRequestURI();
        if (!path.contains("/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        RequestCounter counter = requestCounters.computeIfAbsent(
                clientIp,
                k -> new RequestCounter(now)
        );

        synchronized (counter) {
            if (now - counter.windowStart > WINDOW_SIZE_MS) {
                counter.reset(now);
            }

            counter.count++;

            if (counter.count > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Limite de débit dépassée pour l'IP: {}", clientIp);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Trop de requêtes\",\"message\":\"Veuillez réessayer plus tard\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RequestCounter {
        long windowStart;
        int count;

        RequestCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 0;
        }

        void reset(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.count = 0;
        }
    }
}