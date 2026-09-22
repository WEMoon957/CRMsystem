package com.saas.crm.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪：为每个请求生成/透传 traceId，写入 MDC（日志可见）并回写响应头 X-Request-Id。
 * 客户端/网关排障时凭该 ID 可直接定位日志。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = sanitize(request.getHeader(HEADER));
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** 外部传入的 ID 只接受安全字符，防止日志注入；超长截断 */
    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.replaceAll("[^a-zA-Z0-9\\-]", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > 64 ? cleaned.substring(0, 64) : cleaned;
    }
}
