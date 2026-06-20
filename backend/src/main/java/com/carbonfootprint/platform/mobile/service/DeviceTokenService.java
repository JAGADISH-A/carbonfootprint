package com.carbonfootprint.platform.mobile.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class DeviceTokenService {

    // In a real app, this should be injected from configuration (e.g. application.yml).
    // Using a hardcoded stub key for Phase 4.4 implementation purposes.
    private static final String SECRET_KEY_STRING = "carbonwise-super-secret-key-for-mobile-devices-44!";
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8));
    
    // Tokens expire quickly (e.g. 1 hour) to require frequent refresh.
    public static final long TOKEN_EXPIRY_SECONDS = 3600;

    /**
     * Generates a short-lived JWT for the paired device.
     */
    public String generateDeviceToken(String deviceId, String userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(TOKEN_EXPIRY_SECONDS, ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject(deviceId)
                .claim("userId", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * Validates a token and returns the parsed Claims if valid.
     */
    public Optional<Claims> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception e) {
            log.warn("Invalid device token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
