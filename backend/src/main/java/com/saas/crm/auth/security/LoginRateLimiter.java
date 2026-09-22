package com.saas.crm.auth.security;

import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录限流（防暴力破解）：同一 IP+用户名 在滑动窗口内连续失败次数超限即拒绝。
 * 纯内存实现，单实例部署零依赖；多实例水平扩展时应替换为 Redis 等共享存储。
 */
@Slf4j
@Component
public class LoginRateLimiter {

    /** 窗口时长：10 分钟 */
    private static final long WINDOW_MILLIS = 10 * 60 * 1000L;
    /** 窗口内允许的最大失败次数 */
    private static final int MAX_FAILURES = 5;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    /** 登录前校验：窗口内失败超限则直接拒绝 */
    public void check(String key) {
        AttemptWindow window = attempts.get(key);
        if (window != null && window.isBlocked()) {
            log.warn("login blocked by rate limiter: {}", mask(key));
            throw new BizException(ErrorCode.LOGIN_TOO_FREQUENT);
        }
    }

    /** 记录一次失败 */
    public void recordFailure(String key) {
        attempts.compute(key, (k, old) -> {
            AttemptWindow window = (old == null || old.expired()) ? new AttemptWindow() : old;
            window.failures.incrementAndGet();
            return window;
        });
        // 惰性清理，防止 map 无限增长
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(e -> e.getValue().expired());
        }
    }

    /** 登录成功：清除该 key 的失败记录 */
    public void reset(String key) {
        attempts.remove(key);
    }

    /** 日志中隐藏用户名，只留 IP 维度 */
    private String mask(String key) {
        int idx = key.indexOf('|');
        return idx > 0 ? key.substring(0, idx) + "|***" : "***";
    }

    private static class AttemptWindow {
        private final long windowStart = System.currentTimeMillis();
        private final AtomicInteger failures = new AtomicInteger(0);

        boolean expired() {
            return System.currentTimeMillis() - windowStart > WINDOW_MILLIS;
        }

        boolean isBlocked() {
            return !expired() && failures.get() >= MAX_FAILURES;
        }
    }
}
