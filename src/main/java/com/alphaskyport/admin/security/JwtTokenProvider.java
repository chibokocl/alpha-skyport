package com.alphaskyport.admin.security;

import com.alphaskyport.admin.model.AdminUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
@SuppressWarnings("null")
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REVOKED_TOKEN_PREFIX = "revoked_token:";

    public JwtTokenProvider(
            @Value("${admin.jwt.secret}") String secret,
            @Value("${admin.jwt.access-token-expiration:3600000}") long accessTokenExpiration,
            @Value("${admin.jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration,
            RedisTemplate<String, String> redisTemplate) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(AdminUser admin) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(admin.getAdminId().toString())
                .claim("email", admin.getEmail())
                .claim("role", admin.getRole().name())
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(AdminUser admin) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(admin.getAdminId().toString())
                .claim("type", "refresh")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public UUID getAdminIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseToken(token);
            String type = claims.get("type", String.class);

            if (!"access".equals(type)) {
                return false;
            }

            if (isTokenRevoked(token)) {
                return false;
            }

            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            String type = claims.get("type", String.class);

            if (!"refresh".equals(type)) {
                return false;
            }

            if (isTokenRevoked(token)) {
                return false;
            }

            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    public void revokeToken(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();

            if (ttl > 0) {
                String key = REVOKED_TOKEN_PREFIX + token.hashCode();
                redisTemplate.opsForValue().set(key, "revoked", Duration.ofMillis(ttl));
            }
        } catch (Exception e) {
            log.error("Failed to revoke token: {}", e.getMessage());
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration / 1000; // Return in seconds
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenRevoked(String token) {
        String key = REVOKED_TOKEN_PREFIX + token.hashCode();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
