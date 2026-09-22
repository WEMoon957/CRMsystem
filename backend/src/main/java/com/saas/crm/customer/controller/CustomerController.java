package com.saas.crm.customer.controller;

import com.saas.crm.auth.security.CurrentPrincipal;
import com.saas.crm.common.dto.ApiResponse;
import com.saas.crm.customer.dto.CustomerSaveRequest;
import com.saas.crm.customer.dto.CustomerVO;
import com.saas.crm.customer.dto.PageResult;
import com.saas.crm.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ApiResponse<PageResult<CustomerVO>> page(@AuthenticationPrincipal CurrentPrincipal principal,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String intentLevel,
                                                    @RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(customerService.page(principal.userId(), principal.isAdmin(),
                keyword, intentLevel, page, Math.min(size, 100)));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats(@AuthenticationPrincipal CurrentPrincipal principal) {
        return ApiResponse.ok(customerService.intentStats(principal.userId(), principal.isAdmin()));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerVO> detail(@AuthenticationPrincipal CurrentPrincipal principal,
                                          @PathVariable Long id) {
        return ApiResponse.ok(customerService.detail(principal.userId(), principal.isAdmin(), id));
    }

    @PostMapping
    public ApiResponse<CustomerVO> create(@AuthenticationPrincipal CurrentPrincipal principal,
                                          @Valid @RequestBody CustomerSaveRequest request) {
        return ApiResponse.ok(customerService.create(principal.userId(), principal.isAdmin(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerVO> update(@AuthenticationPrincipal CurrentPrincipal principal,
                                          @PathVariable Long id,
                                          @Valid @RequestBody CustomerSaveRequest request) {
        return ApiResponse.ok(customerService.update(principal.userId(), principal.isAdmin(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentPrincipal principal,
                                    @PathVariable Long id) {
        customerService.delete(principal.userId(), principal.isAdmin(), id);
        return ApiResponse.ok();
    }

    /** 主账号专属：客户分配 */
    @PutMapping("/{id}/assign")
    public ApiResponse<CustomerVO> assign(@AuthenticationPrincipal CurrentPrincipal principal,
                                         @PathVariable Long id,
                                         @RequestBody Map<String, Long> body) {
        return ApiResponse.ok(customerService.assign(principal.userId(), principal.isAdmin(), id, body.get("ownerId")));
    }
}
