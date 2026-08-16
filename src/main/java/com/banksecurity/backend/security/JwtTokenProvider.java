package com.banksecurity.backend.security;

import com.banksecurity.backend.util.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final long jwtExpiration = Constants.JWT_EXPIRATION;
    private final long refreshExpiration = Constants.JWT_REFRESH_EXPIRATION;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new IllegalArgumentException("Authentication invalide");
        }
        return generateToken(userPrincipal);
    }

    public String generateToken(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new IllegalArgumentException("UserPrincipal ne peut pas être null");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.getId().toString());
        claims.put("email", userPrincipal.getEmail());
        claims.put("firstName", userPrincipal.getFirstName());
        claims.put("lastName", userPrincipal.getLastName());
        claims.put("role", userPrincipal.getRole().name());
        // ✅ Utilisation de Constants.JWT_TYPE
        claims.put("type", Constants.JWT_TYPE);

        return createToken(claims, userPrincipal.getUsername(), jwtExpiration);
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new IllegalArgumentException("UserPrincipal ne peut pas être null");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userPrincipal.getId().toString());
        claims.put("type", "refresh");

        return createToken(claims, userPrincipal.getUsername(), refreshExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public UUID getUserIdFromToken(String token) {
        String userId = extractClaim(token, claims -> claims.get("userId", String.class));
        if (userId == null) {
            throw new IllegalArgumentException("Claim 'userId' manquante dans le token");
        }
        return UUID.fromString(userId);
    }

    public String getRoleFromToken(String token) {
        String role = extractClaim(token, claims -> claims.get("role", String.class));
        return role != null ? role : "UNKNOWN";
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Signature JWT invalide: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Token JWT malformé: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Token JWT expiré: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Token JWT non supporté: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Token JWT vide ou invalide: {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public long getTokenValidityInSeconds() {
        return jwtExpiration / 1000;
    }

    public long getRefreshTokenValidityInSeconds() {
        return refreshExpiration / 1000;
    }
}