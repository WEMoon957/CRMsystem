package com.saas.crm.auth.security;

/**
 * 认证后的请求主体（来自 JWT claims，过滤器中已与数据库核对）。
 */
public record CurrentPrincipal(Long userId, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
