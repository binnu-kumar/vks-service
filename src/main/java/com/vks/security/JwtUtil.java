package com.vks.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:900000}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthenticatedUser user) {
        return buildToken(user, expirationMs, "access");
    }

    public String generateRefreshToken(AuthenticatedUser user) {
        return buildToken(user, refreshExpirationMs, "refresh");
    }

    public String generatePasswordResetToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .claim("type", "password-reset")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private String buildToken(AuthenticatedUser user, long expiry, String type) {
        return Jwts.builder()
                .subject(user.userId())
                .claim("username", user.username())
                .claim("tenant_id", user.tenantId())
                .claim("role", user.role().name())
                .claim("scope", user.scopes())
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseClaims(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("type", String.class));
    }

    public boolean isPasswordResetToken(String token) {
        return "password-reset".equals(parseClaims(token).get("type", String.class));
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public AuthenticatedUser extractAuthenticatedUser(String token) {
        Claims claims = parseClaims(token);
        String roleValue = claims.get("role", String.class);
        UserRole role = roleValue != null ? UserRole.valueOf(roleValue) : UserRole.CUSTOMER;
        List<String> scopes = claims.get("scope", List.class);

        return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("username", String.class),
                claims.get("tenant_id", String.class),
                role,
                scopes != null ? scopes : role.defaultScopes()
        );
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
