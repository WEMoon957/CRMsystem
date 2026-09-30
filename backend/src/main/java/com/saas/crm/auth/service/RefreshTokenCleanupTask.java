package com.saas.crm.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.crm.auth.entity.RefreshToken;
import com.saas.crm.auth.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 过期刷新令牌清理：避免 sys_refresh_token 无限膨胀。
 * 默认每小时执行一次，可用 app.token-cleanup-cron 覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private final RefreshTokenMapper refreshTokenMapper;

    @Scheduled(cron = "${app.token-cleanup-cron:0 5 * * * ?}")
    public void purgeExpired() {
        int removed = refreshTokenMapper.delete(new LambdaQueryWrapper<RefreshToken>()
                .lt(RefreshToken::getExpiresAt, LocalDateTime.now()));
        if (removed > 0) {
            log.info("purged {} expired refresh tokens", removed);
        }
    }
}
