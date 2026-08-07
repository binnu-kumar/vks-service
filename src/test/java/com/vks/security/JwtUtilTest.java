package com.vks.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @Test
    void generatesAndValidatesAccessRefreshAndResetTokens() {
        JwtUtil jwtUtil = configuredJwtUtil();

        String accessToken = jwtUtil.generateToken("user-1");
        String refreshToken = jwtUtil.generateRefreshToken("user-1");
        String resetToken = jwtUtil.generatePasswordResetToken("user-1");

        assertTrue(jwtUtil.validateToken(accessToken));
        assertTrue(jwtUtil.validateToken(refreshToken));
        assertTrue(jwtUtil.validateToken(resetToken));
        assertEquals("user-1", jwtUtil.extractSubject(accessToken));
        assertTrue(jwtUtil.isRefreshToken(refreshToken));
        assertFalse(jwtUtil.isRefreshToken(accessToken));
        assertTrue(jwtUtil.isPasswordResetToken(resetToken));
        assertFalse(jwtUtil.isPasswordResetToken(accessToken));
    }

    @Test
    void validateTokenReturnsFalseForTamperedToken() {
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = jwtUtil.generateToken("user-1") + "tampered";

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