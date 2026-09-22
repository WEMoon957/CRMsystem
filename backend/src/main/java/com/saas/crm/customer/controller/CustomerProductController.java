package com.saas.crm.customer.controller;

import com.saas.crm.auth.security.CurrentPrincipal;
import com.saas.crm.common.dto.ApiResponse;
import com.saas.crm.customer.dto.ProductRequest;
import com.saas.crm.customer.entity.Product;
import com.saas.crm.customer.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户产品资料（客户子资源，继承数据隔离）。
 */
@RestController
@RequestMapping("/api/customers/{customerId}/products")
@RequiredArgsConstructor
public class CustomerProductController {

    private final ProductService productService;

    @GetMapping
    public ApiResponse<List<Product>> list(@AuthenticationPrincipal CurrentPrincipal principal,
                                           @PathVariable Long customerId) {
        return ApiResponse.ok(productService.list(principal.userId(), principal.isAdmin(), customerId));
    }

    @PostMapping
    public ApiResponse<Product> create(@AuthenticationPrincipal CurrentPrincipal principal,
                                       @PathVariable Long customerId,
                                       @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.create(principal.userId(), principal.isAdmin(), customerId, request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<Product> update(@AuthenticationPrincipal CurrentPrincipal principal,
                                       @PathVariable Long customerId,
                                       @PathVariable Long productId,
                                       @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(productService.update(principal.userId(), principal.isAdmin(), customerId, productId, request));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CurrentPrincipal principal,
                                    @PathVariable Long customerId,
                                    @PathVariable Long productId) {
        productService.delete(principal.userId(), principal.isAdmin(), customerId, productId);
        return ApiResponse.ok();
    }
}
