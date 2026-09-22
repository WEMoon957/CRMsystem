package com.saas.crm.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtService 纯单元测试：签发/解析闭环、篡改拒绝、过期拒绝。
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-which-is-long-enough-0123456789abcdef");
        ReflectionTestUtils.setField(jwtService, "accessTokenTtlMinutes", 120L);
        jwtService.init();
    }

    @Test
    void generateThenParseRoundTrip() {
        String token = jwtService.generateAccessToken(7L, "admin", "ADMIN");

        Claims claims = jwtService.parse(token);

        assertNotNull(claims);
        assertEquals("7", claims.getSubject());
        assertEquals("admin", claims.get("username"));
        assertEquals("ADMIN", claims.get("role"));
        assertTrue(claims.getExpiration().getTime() > System.currentTimeMillis());
    }

    @Test
    void parseReturnsNullForTamperedToken() {
        String token = jwtService.generateAccessToken(7L, "admin", "ADMIN");
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertNull(jwtService.parse(tampered));
    }

    @Test
    void parseReturnsNullForGarbage() {
        assertNull(jwtService.parse("not-a-jwt"));
        assertNull(jwtService.parse(""));
    }

    @Test
    void parseReturnsNullForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenTtlMinutes", -10L);
        jwtService.init();
        String expired = jwtService.generateAccessToken(7L, "admin", "ADMIN");

        assertNull(jwtService.parse(expired));
    }

    @Test
    void assertValidSubjectRejectsBadIds() {
        assertThrows(Exception.class, () -> jwtService.assertValidSubject(null));
        assertThrows(Exception.class, () -> jwtService.assertValidSubject(0L));
        assertDoesNotThrow(() -> jwtService.assertValidSubject(1L));
    }
}
