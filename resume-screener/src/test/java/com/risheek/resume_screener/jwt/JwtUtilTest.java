package com.risheek.resume_screener.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhzNTEy" +
                    "LWFsZ29yaXRobS10b3Bhc3MtdGhlLW1pbmltdW0tc2l6ZS1yZXF1aXJlbWVudA==";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullNonBlankToken() {
        String token = jwtUtil.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsSameUsernameUsedToGenerate() {
        String token = jwtUtil.generateToken("user@example.com");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void isTokenValid_validTokenAndMatchingUsername_returnsTrue() {
        String token = jwtUtil.generateToken("user@example.com");
        assertThat(jwtUtil.isTokenValid(token, "user@example.com")).isTrue();
    }

    @Test
    void isTokenValid_validTokenButWrongUsername_returnsFalse() {
        String token = jwtUtil.generateToken("user@example.com");
        assertThat(jwtUtil.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void isTokenExpired_freshToken_returnsFalse() {
        String token = jwtUtil.generateToken("user@example.com");
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_expiredToken_throwsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1L);
        String token = jwtUtil.generateToken("user@example.com");
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.isTokenExpired(token));
    }

    @Test
    void extractUsername_tamperedToken_throwsException() {
        String token = jwtUtil.generateToken("user@example.com");
        String tampered = token + "TAMPERED";
        assertThrows(Exception.class, () -> jwtUtil.extractUsername(tampered));
    }
}
