package com.saas.crm.followup.controller;

import com.saas.crm.auth.security.CurrentPrincipal;
import com.saas.crm.common.dto.ApiResponse;
import com.saas.crm.followup.service.FollowUpService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers/{customerId}/followups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@AuthenticationPrincipal CurrentPrincipal principal,
                                                       @PathVariable Long customerId) {
        return ApiResponse.ok(followUpService.list(principal.userId(), principal.isAdmin(), customerId));
    }

    @PostMapping
    public ApiResponse<Void> create(@AuthenticationPrincipal CurrentPrincipal principal,
                                    @PathVariable Long customerId,
                                    @jakarta.validation.Valid @RequestBody FollowUpRequest request) {
        followUpService.create(principal.userId(), principal.isAdmin(), customerId,
                request.getContent(), request.getResultLevel(), request.getNextFollowupAt());
        return ApiResponse.ok();
    }

    @Data
    public static class FollowUpRequest {

        @NotBlank(message = "跟进内容不能为空")
        @Size(max = 2048, message = "跟进内容过长")
        private String content;

        /** 本次跟进得出的意向结论（可选） */
        private String resultLevel;

        private LocalDateTime nextFollowupAt;
    }
}
