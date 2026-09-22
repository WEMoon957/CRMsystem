package com.saas.crm.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.saas.crm.auth.entity.User;
import com.saas.crm.auth.mapper.UserMapper;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import com.saas.crm.customer.dto.CustomerSaveRequest;
import com.saas.crm.customer.dto.CustomerVO;
import com.saas.crm.customer.dto.PageResult;
import com.saas.crm.customer.entity.Customer;
import com.saas.crm.customer.entity.IntentLevel;
import com.saas.crm.customer.mapper.CustomerMapper;
import com.saas.crm.followup.entity.FollowUp;
import com.saas.crm.followup.mapper.FollowUpMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客户服务：核心规则 ——
 * 1. 子账号（MEMBER）只能查询/修改/删除自己名下（owner_id = 自己）的客户；
 * 2. 主账号（ADMIN）全量可见，可将客户分配给任意子账号。
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final FollowUpMapper followUpMapper;

    public PageResult<CustomerVO> page(Long currentUserId, boolean isAdmin,
                                       String keyword, String intentLevel,
                                       long page, long size) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(Customer::getOwnerId, currentUserId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Customer::getName, kw)
                    .or().like(Customer::getPhone, kw)
                    .or().like(Customer::getCompany, kw));
        }
        if (StringUtils.hasText(intentLevel)) {
            IntentLevel.of(intentLevel);
            wrapper.eq(Customer::getIntentLevel, intentLevel);
        }
        wrapper.orderByDesc(Customer::getUpdatedAt);

        Page<Customer> result = customerMapper.selectPage(new Page<>(page, size), wrapper);
        List<CustomerVO> vos = toVos(result.getRecords());
        return PageResult.of(vos, result.getTotal(), result.getCurrent(), result.getSize());
    }

    public CustomerVO detail(Long currentUserId, boolean isAdmin, Long id) {
        Customer customer = getAccessibleCustomer(currentUserId, isAdmin, id);
        return toVos(List.of(customer)).get(0);
    }

    @Transactional
    public CustomerVO create(Long currentUserId, boolean isAdmin, CustomerSaveRequest request) {
        IntentLevel.of(request.getIntentLevel());

        Customer customer = new Customer();
        applySaveRequest(customer, request);
        customer.setOwnerId(resolveOwner(currentUserId, isAdmin, request.getOwnerId()));
        customer.setCreatedBy(currentUserId);
        customer.setLastContactedAt(null);
        customerMapper.insert(customer);
        return toVos(List.of(customer)).get(0);
    }

    @Transactional
    public CustomerVO update(Long currentUserId, boolean isAdmin, Long id, CustomerSaveRequest request) {
        IntentLevel.of(request.getIntentLevel());
        Customer customer = getAccessibleCustomer(currentUserId, isAdmin, id);

        applySaveRequest(customer, request);
        // 归属变更：仅主账号可转移
        if (request.getOwnerId() != null && isAdmin) {
            customer.setOwnerId(resolveOwner(currentUserId, isAdmin, request.getOwnerId()));
        }
        customerMapper.updateById(customer);
        return toVos(List.of(customer)).get(0);
    }

    @Transactional
    public void delete(Long currentUserId, boolean isAdmin, Long id) {
        Customer customer = getAccessibleCustomer(currentUserId, isAdmin, id);
        customerMapper.deleteById(customer.getId());
        followUpMapper.delete(new LambdaQueryWrapper<FollowUp>().eq(FollowUp::getCustomerId, customer.getId()));
    }

    /** 主账号将客户分配给子账号（或收回给自己） */
    @Transactional
    public CustomerVO assign(Long operatorId, boolean isAdmin, Long customerId, Long targetOwnerId) {
        if (!isAdmin) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        customer.setOwnerId(resolveOwner(operatorId, isAdmin, targetOwnerId));
        customerMapper.updateById(customer);
        return toVos(List.of(customer)).get(0);
    }

    /** 意向分布统计：主账号全局，子账号仅自己名下 */
    public Map<String, Long> intentStats(Long currentUserId, boolean isAdmin) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            wrapper.eq(Customer::getOwnerId, currentUserId);
        }
        wrapper.select(Customer::getIntentLevel);
        List<Customer> rows = customerMapper.selectList(wrapper);
        return rows.stream().collect(Collectors.groupingBy(
                c -> c.getIntentLevel() == null ? "PENDING" : c.getIntentLevel(),
                Collectors.counting()));
    }

    /** 数据隔离守卫：任何单条客户操作前必须调用 */
    private Customer getAccessibleCustomer(Long currentUserId, boolean isAdmin, Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        if (!isAdmin && !currentUserId.equals(customer.getOwnerId())) {
            throw new BizException(ErrorCode.CUSTOMER_NOT_OWNED);
        }
        return customer;
    }

    /** 仅校验归属权限（供产品/附件等子资源服务复用） */
    public void checkAccess(Long currentUserId, boolean isAdmin, Long customerId) {
        getAccessibleCustomer(currentUserId, isAdmin, customerId);
    }

    private void applySaveRequest(Customer customer, CustomerSaveRequest request) {
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setCompany(request.getCompany());
        customer.setPosition(request.getPosition());
        customer.setSource(request.getSource());
        customer.setIndustry(request.getIndustry());
        customer.setIntentLevel(request.getIntentLevel());
        customer.setAddress(request.getAddress());
        customer.setRemark(request.getRemark());
        customer.setNextFollowupAt(request.getNextFollowupAt());
    }

    private Long resolveOwner(Long currentUserId, boolean isAdmin, Long requestedOwnerId) {
        if (!isAdmin || requestedOwnerId == null) {
            return currentUserId;
        }
        User owner = userMapper.selectById(requestedOwnerId);
        if (owner == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "指定的归属账号不存在");
        }
        return owner.getId();
    }

    private List<CustomerVO> toVos(List<Customer> customers) {
        if (customers.isEmpty()) {
            return List.of();
        }
        List<Long> ownerIds = customers.stream()
                .map(Customer::getOwnerId).distinct().toList();
        Map<Long, String> ownerNames = userMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        return customers.stream().map(c -> {
            IntentLevel level = IntentLevel.of(c.getIntentLevel() == null ? "PENDING" : c.getIntentLevel());
            return CustomerVO.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .phone(c.getPhone())
                    .company(c.getCompany())
                    .position(c.getPosition())
                    .source(c.getSource())
                    .industry(c.getIndustry())
                    .intentLevel(level.name())
                    .intentLabel(level.getLabel())
                    .intentTone(level.getTone())
                    .address(c.getAddress())
                    .remark(c.getRemark())
                    .ownerId(c.getOwnerId())
                    .ownerName(ownerNames.getOrDefault(c.getOwnerId(), "未知"))
                    .nextFollowupAt(c.getNextFollowupAt())
                    .lastContactedAt(c.getLastContactedAt())
                    .createdAt(c.getCreatedAt())
                    .updatedAt(c.getUpdatedAt())
                    .build();
        }).toList();
    }
}
