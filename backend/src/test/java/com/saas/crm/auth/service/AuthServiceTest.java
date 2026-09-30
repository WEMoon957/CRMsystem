package com.saas.crm.auth.service;

import com.saas.crm.auth.dto.LoginRequest;
import com.saas.crm.auth.dto.LoginResponse;
import com.saas.crm.auth.entity.RefreshToken;
import com.saas.crm.auth.entity.User;
import com.saas.crm.auth.mapper.RefreshTokenMapper;
import com.saas.crm.auth.mapper.UserMapper;
import com.saas.crm.auth.security.LoginRateLimiter;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试（Mockito，不依赖数据库与 Spring 容器）：
 * 覆盖登录失败/成功、禁用账号、限流联动、刷新令牌轮换。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTtlDays", 7L);
    }

    private User enabledUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("hashed");
        user.setRole(User.ROLE_ADMIN);
        user.setStatus(1);
        return user;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123");
        return request;
    }

    @Test
    void loginFailsWithWrongPasswordAndRecordsFailure() {
        when(userMapper.selectOne(any())).thenReturn(enabledUser());
        when(passwordEncoder.matches("Admin@123", "hashed")).thenReturn(false);

        BizException e = assertThrows(BizException.class,
                () -> authService.login(loginRequest(), "127.0.0.1"));

        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), e.getCode());
        verify(loginRateLimiter).recordFailure("127.0.0.1|admin");
        verify(loginRateLimiter, never()).reset(any());
    }

    @Test
    void loginFailsForUnknownUserAndRecordsFailure() {
        when(userMapper.selectOne(any())).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> authService.login(loginRequest(), "127.0.0.1"));

        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), e.getCode());
        verify(loginRateLimiter).recordFailure("127.0.0.1|admin");
    }

    @Test
    void loginFailsForDisabledAccount() {
        User user = enabledUser();
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        BizException e = assertThrows(BizException.class,
                () -> authService.login(loginRequest(), "127.0.0.1"));

        assertEquals(ErrorCode.ACCOUNT_DISABLED.getCode(), e.getCode());
    }

    @Test
    void loginSuccessIssuesTokensAndResetsLimiter() {
        when(userMapper.selectOne(any())).thenReturn(enabledUser());
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.generateAccessToken(1L, "admin", "ADMIN")).thenReturn("access-token");

        LoginResponse response = authService.login(loginRequest(), "127.0.0.1");

        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(64, response.getRefreshToken().length());
        assertEquals("admin", response.getUser().getUsername());
        verify(refreshTokenMapper).insert(any(RefreshToken.class));
        verify(loginRateLimiter).reset("127.0.0.1|admin");
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(refreshTokenMapper.selectOne(any())).thenReturn(null);

        BizException e = assertThrows(BizException.class, () -> authService.refresh("bad-token"));

        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID.getCode(), e.getCode());
    }

    @Test
    void refreshRejectsExpiredToken() {
        RefreshToken stored = new RefreshToken();
        stored.setId(9L);
        stored.setUserId(1L);
        stored.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(refreshTokenMapper.selectOne(any())).thenReturn(stored);

        BizException e = assertThrows(BizException.class, () -> authService.refresh("old-token"));

        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID.getCode(), e.getCode());
    }

    @Test
    void refreshRotatesOldTokenAndIssuesNewPair() {
        RefreshToken stored = new RefreshToken();
        stored.setId(9L);
        stored.setUserId(1L);
        stored.setToken("old-token");
        stored.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenMapper.selectOne(any())).thenReturn(stored);
        when(userMapper.selectById(1L)).thenReturn(enabledUser());
        when(jwtService.generateAccessToken(1L, "admin", "ADMIN")).thenReturn("new-access");

        LoginResponse response = authService.refresh("old-token");

        assertEquals("new-access", response.getAccessToken());
        assertNotEquals("old-token", response.getRefreshToken());
        verify(refreshTokenMapper).deleteById(9L);
        verify(refreshTokenMapper).insert(any(RefreshToken.class));
    }
}
