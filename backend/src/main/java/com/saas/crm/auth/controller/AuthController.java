package com.saas.crm.auth.controller;

import com.saas.crm.auth.dto.LoginRequest;
import com.saas.crm.auth.dto.LoginResponse;
import com.saas.crm.auth.dto.UserVO;
import com.saas.crm.auth.security.CurrentPrincipal;
import com.saas.crm.auth.service.AuthService;
import com.saas.crm.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest http) {
        return ApiResponse.ok(authService.login(request, clientIp(http)));
    }

    /** 反向代理后取 X-Forwarded-For 首跳；直连取 remoteAddr */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.refresh(body.get("refreshToken")));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) Map<String, String> body) {
        authService.logout(body == null ? null : body.get("refreshToken"));
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@AuthenticationPrincipal CurrentPrincipal principal) {
        return ApiResponse.ok(authService.me(principal.userId()));
    }
}
