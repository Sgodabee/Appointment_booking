package com.appointmentbooking.security;

import com.appointmentbooking.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_NAME = "name";

    public static final String ROLE_EMPLOYEE = "EMPLOYEE";
    public static final String ROLE_ADMIN    = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private final AppProperties appProperties;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        String secret = appProperties.getJwt().getSecret();
        byte[] keyBytes;
        try {
            // Accept raw or Base64-encoded secret
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        // Keys.hmacShaKeyFor requires ≥ 32 bytes for HS256
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JwtService initialised (key length: {} bytes)", keyBytes.length);
    }

    // ── Token generation ────────────────────────────────────────────────────

    /** Issue a token for a branch employee / admin. */
    public String generateEmployeeToken(Long id, String username, String fullName, String role) {
        long expiryMs = appProperties.getJwt().getEmployeeExpirySeconds() * 1_000L;
        return build(String.valueOf(id), Map.of(
                CLAIM_ROLE, role,
                CLAIM_NAME, fullName,
                "username", username
        ), expiryMs);
    }

    /** Issue a short-lived token for an authenticated Capitec customer. */
    public String generateCustomerToken(String idNumber, String fullName) {
        long expiryMs = appProperties.getJwt().getCustomerExpirySeconds() * 1_000L;
        return build(idNumber, Map.of(
                CLAIM_ROLE, ROLE_CUSTOMER,
                CLAIM_NAME, fullName
        ), expiryMs);
    }

    private String build(String subject, Map<String, Object> extraClaims, long expiryMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .claims(extraClaims)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(signingKey)
                .compact();
    }

    // ── Token validation & extraction ────────────────────────────────────────

    /**
     * Parse and validate a JWT. Returns the Claims on success.
     * Throws JwtException (or subclass) if invalid/expired.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Returns true only if the token parses and is not expired. */
    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String extractSubject(String token) {
        return parseAndValidate(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) parseAndValidate(token).get(CLAIM_ROLE);
    }

    public String extractName(String token) {
        return (String) parseAndValidate(token).get(CLAIM_NAME);
    }
}
