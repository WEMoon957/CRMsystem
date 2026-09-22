package com.saas.crm.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import com.saas.crm.customer.dto.ProductRequest;
import com.saas.crm.customer.entity.Product;
import com.saas.crm.customer.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 客户产品资料（客户子资源，继承客户级数据隔离）。
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final CustomerService customerService;

    public List<Product> list(Long currentUserId, boolean isAdmin, Long customerId) {
        customerService.checkAccess(currentUserId, isAdmin, customerId);
        return productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getCustomerId, customerId)
                .orderByDesc(Product::getId));
    }

    @Transactional
    public Product create(Long currentUserId, boolean isAdmin, Long customerId, ProductRequest request) {
        customerService.checkAccess(currentUserId, isAdmin, customerId);
        Product product = new Product();
        apply(product, request);
        product.setCustomerId(customerId);
        productMapper.insert(product);
        return product;
    }

    @Transactional
    public Product update(Long currentUserId, boolean isAdmin, Long customerId, Long productId, ProductRequest request) {
        customerService.checkAccess(currentUserId, isAdmin, customerId);
        Product product = productMapper.selectById(productId);
        if (product == null || !customerId.equals(product.getCustomerId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "产品资料不存在");
        }
        apply(product, request);
        productMapper.updateById(product);
        return product;
    }

    @Transactional
    public void delete(Long currentUserId, boolean isAdmin, Long customerId, Long productId) {
        customerService.checkAccess(currentUserId, isAdmin, customerId);
        Product product = productMapper.selectById(productId);
        if (product == null || !customerId.equals(product.getCustomerId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "产品资料不存在");
        }
        productMapper.deleteById(productId);
    }

    private void apply(Product product, ProductRequest request) {
        product.setProductName(request.getProductName());
        product.setSpec(request.getSpec());
        product.setQuantity(request.getQuantity());
        product.setUnitPrice(request.getUnitPrice());
        product.setRemark(request.getRemark());
    }
}
