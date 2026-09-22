package com.saas.crm.followup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.crm.customer.entity.Customer;
import com.saas.crm.customer.mapper.CustomerMapper;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import com.saas.crm.followup.entity.FollowUp;
import com.saas.crm.followup.mapper.FollowUpMapper;
import com.saas.crm.auth.entity.User;
import com.saas.crm.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 跟进记录服务：与客户相同的隔离规则（子账号只能操作自己名下客户的记录）。
 * 新增跟进时同步更新客户的意向等级与最近联系时间 —— 数据一处录入。
 */
@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpMapper followUpMapper;
    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;

    public List<Map<String, Object>> list(Long currentUserId, boolean isAdmin, Long customerId) {
        checkAccess(currentUserId, isAdmin, customerId);
        List<FollowUp> records = followUpMapper.selectList(new LambdaQueryWrapper<FollowUp>()
                .eq(FollowUp::getCustomerId, customerId)
                .orderByDesc(FollowUp::getCreatedAt));
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, String> userNames = userMapper.selectBatchIds(
                        records.stream().map(FollowUp::getCreatedBy).filter(id -> id != null).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId,
                        u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        return records.stream().map(r -> Map.<String, Object>of(
                "id", r.getId(),
                "content", r.getContent(),
                "resultLevel", r.getResultLevel() == null ? "" : r.getResultLevel(),
                "nextFollowupAt", r.getNextFollowupAt() == null ? "" : r.getNextFollowupAt().toString(),
                "createdBy", r.getCreatedBy() == null ? "" : userNames.getOrDefault(r.getCreatedBy(), "未知"),
                "createdAt", r.getCreatedAt() == null ? "" : r.getCreatedAt().toString()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void create(Long currentUserId, boolean isAdmin, Long customerId, String content,
                       String resultLevel, LocalDateTime nextFollowupAt) {
        Customer customer = checkAccess(currentUserId, isAdmin, customerId);

        FollowUp record = new FollowUp();
        record.setCustomerId(customerId);
        record.setContent(content);
        record.setResultLevel(resultLevel);
        record.setNextFollowupAt(nextFollowupAt);
        record.setCreatedBy(currentUserId);
        followUpMapper.insert(record);

        // 跟进结论回写客户：意向等级、下次跟进时间、最近联系时间
        if (resultLevel != null && !resultLevel.isBlank()) {
            customer.setIntentLevel(resultLevel);
        }
        customer.setNextFollowupAt(nextFollowupAt);
        customer.setLastContactedAt(LocalDateTime.now());
        customerMapper.updateById(customer);
    }

    private Customer checkAccess(Long currentUserId, boolean isAdmin, Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
        if (!isAdmin && !currentUserId.equals(customer.getOwnerId())) {
            throw new BizException(ErrorCode.CUSTOMER_NOT_OWNED);
        }
        return customer;
    }
}
