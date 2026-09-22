package com.saas.crm.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.crm.auth.dto.LoginRequest;
import com.saas.crm.auth.dto.LoginResponse;
import com.saas.crm.auth.dto.UserVO;
import com.saas.crm.auth.entity.RefreshToken;
import com.saas.crm.auth.entity.User;
import com.saas.crm.auth.mapper.RefreshTokenMapper;
import com.saas.crm.auth.mapper.UserMapper;
import com.saas.crm.auth.security.LoginRateLimiter;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter loginRateLimiter;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-token-ttl-days}")
    private long refreshTtlDays;

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        String rateKey = clientIp + "|" + request.getUsername();
        loginRateLimiter.check(rateKey);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginRateLimiter.recordFailure(rateKey);
            throw new BizException(ErrorCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!user.enabled()) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        loginRateLimiter.reset(rateKey);
        return issueTokens(user);
    }

    /**
     * 刷新令牌轮换：旧 refreshToken 一次性作废，签发全新令牌对。
     */
    @Transactional
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        RefreshToken stored = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getToken, refreshToken));
        if (stored == null || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        User user = userMapper.selectById(stored.getUserId());
        if (user == null || !user.enabled()) {
            throw new BizException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        refreshTokenMapper.deleteById(stored.getId());
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenMapper.delete(new LambdaQueryWrapper<RefreshToken>()
                    .eq(RefreshToken::getToken, refreshToken));
        }
    }

    public UserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return UserVO.from(user);
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = newToken();

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setToken(refreshToken);
        entity.setExpiresAt(LocalDateTime.now().plusDays(refreshTtlDays));
        refreshTokenMapper.insert(entity);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserVO.from(user))
                .build();
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
