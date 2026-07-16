package com.plm.service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Issues and validates the HS256 JWTs used by both the GraphQL API and the chatbot REST
 * endpoint. Both processes are configured with the same shared secret so a token minted by
 * one is honored by the other.
 */
public class JwtUtil {

    private static final String ROLES_CLAIM = "roles";

    private final SecretKey key;
    private final Duration ttl;

    public JwtUtil(String secret, Duration ttl) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    public String issue(String username, Set<Role> roles) {
        Instant now = Instant.now();
        String rolesCsv = roles.stream().map(Enum::name).collect(Collectors.joining(","));
        return Jwts.builder()
                .subject(username)
                .claim(ROLES_CLAIM, rolesCsv)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Validates the token and returns the principal, or throws JwtException if invalid/expired. */
    public PlmPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String rolesCsv = claims.get(ROLES_CLAIM, String.class);
        Set<Role> roles = rolesCsv == null || rolesCsv.isBlank()
                ? Set.of()
                : Set.of(rolesCsv.split(",")).stream().map(Role::valueOf).collect(Collectors.toSet());
        return new PlmPrincipal(claims.getSubject(), roles);
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
