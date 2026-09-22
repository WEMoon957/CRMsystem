package com.saas.crm.user.controller;

import com.saas.crm.auth.dto.UserVO;
import com.saas.crm.auth.security.CurrentPrincipal;
import com.saas.crm.common.dto.ApiResponse;
import com.saas.crm.user.service.UserManageService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 子账号管理：/api/users/** 已在 SecurityConfig 中限定 ADMIN 角色。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserManageController {

    private final UserManageService userManageService;

    @GetMapping
    public ApiResponse<List<UserVO>> list() {
        return ApiResponse.ok(userManageService.list());
    }

    @PostMapping
    public ApiResponse<UserVO> create(@jakarta.validation.Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userManageService.create(request.getUsername(), request.getPassword(),
                request.getRealName(), request.getPhone(), request.getRole()));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserVO> update(@AuthenticationPrincipal CurrentPrincipal principal,
                                      @PathVariable Long id,
                                      @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userManageService.update(principal.userId(), id,
                request.getRealName(), request.getPhone(), request.getStatus(), request.getNewPassword()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentPrincipal principal,
                                    @PathVariable Long id) {
        userManageService.delete(principal.userId(), id);
        return ApiResponse.ok();
    }

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        private String realName;
        private String phone;
        private String role;
    }

    @Data
    public static class UpdateUserRequest {
        private String realName;
        private String phone;
        private Integer status;
        private String newPassword;
    }
}
