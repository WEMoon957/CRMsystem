package com.saas.crm.auth.security;

import com.saas.crm.auth.entity.User;
import com.saas.crm.auth.mapper.UserMapper;
import com.saas.crm.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Bearer token，与数据库核对账号仍存在且启用后写入 SecurityContext。
 * 禁用/删除的账号即使 token 未过期也会立即失效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            Claims claims = jwtService.parse(header.substring(7));
            if (claims != null) {
                try {
                    Long userId = Long.valueOf(claims.getSubject());
                    User user = userMapper.selectById(userId);
                    if (user != null && user.enabled()) {
                        CurrentPrincipal principal = new CurrentPrincipal(user.getId(), user.getUsername(), user.getRole());
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (NumberFormatException e) {
                    log.warn("invalid jwt subject: {}", claims.getSubject());
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
