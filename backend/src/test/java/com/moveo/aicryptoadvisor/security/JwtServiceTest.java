package com.moveo.aicryptoadvisor.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret-key-that-is-at-least-32-bytes-long");

    @Test
    void generateToken_roundTripsClaimsAndExpiry() {
        UUID userId = UUID.randomUUID();
        String email = "jane@example.com";

        String token = jwtService.generateToken(userId, email);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo(email);

        Instant issuedAt = claims.getIssuedAt().toInstant();
        Instant expiration = claims.getExpiration().toInstant();
        assertThat(Duration.between(issuedAt, expiration)).isCloseTo(Duration.ofHours(24), Duration.ofSeconds(5));
        assertThat(expiration).isAfter(Instant.now());
    }
}
