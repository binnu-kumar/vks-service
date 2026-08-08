package com.vks.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(
            "user-1", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes());

    @Test
    void generatesAndValidatesAccessRefreshAndResetTokens() {
        JwtUtil jwtUtil = configuredJwtUtil();

        String accessToken = jwtUtil.generateToken(USER);
        String refreshToken = jwtUtil.generateRefreshToken(USER);
        String resetToken = jwtUtil.generatePasswordResetToken("user-1");

        assertTrue(jwtUtil.validateToken(accessToken));
        assertTrue(jwtUtil.validateToken(refreshToken));
        assertTrue(jwtUtil.validateToken(resetToken));
        assertEquals("user-1", jwtUtil.extractSubject(accessToken));
        assertEquals("tenant-123", jwtUtil.extractAuthenticatedUser(accessToken).tenantId());
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        assertFalse(jwtUtil.isRefreshToken(accessToken));
        assertTrue(jwtUtil.isPasswordResetToken(resetToken));
        assertFalse(jwtUtil.isPasswordResetToken(accessToken));
    }

    @Test
    void validateTokenReturnsFalseForTamperedToken() {
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = jwtUtil.generateToken(USER) + "tampered";

        assertFalse(jwtUtil.validateToken(token));
    }

    private JwtUtil configuredJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "vks-super-secret-key-must-be-at-least-32-chars");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 900_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", 604_800_000L);
        return jwtUtil;
    }
}