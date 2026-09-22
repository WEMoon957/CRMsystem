package com.saas.crm.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.saas.crm.auth.dto.UserVO;
import com.saas.crm.auth.entity.User;
import com.saas.crm.auth.mapper.UserMapper;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import com.saas.crm.customer.entity.Customer;
import com.saas.crm.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 子账号管理（仅主账号可用，Controller 层已通过 /api/users/** hasRole(ADMIN) 保护）。
 */
@Service
@RequiredArgsConstructor
public class UserManageService {

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserVO> list() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .orderByAsc(User::getId));
        Map<Long, Long> counts = customerMapper.selectList(new LambdaQueryWrapper<Customer>()
                        .select(Customer::getOwnerId))
                .stream().collect(Collectors.groupingBy(Customer::getOwnerId, Collectors.counting()));

        return users.stream().map(u -> {
            UserVO vo = UserVO.from(u);
            vo.setCustomerCount(counts.getOrDefault(u.getId(), 0L).intValue());
            return vo;
        }).toList();
    }

    @Transactional
    public UserVO create(String username, String password, String realName, String phone, String role) {
        if (!StringUtils.hasText(username) || !username.matches("^[a-zA-Z0-9_]{3,32}$")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "用户名需为 3-32 位字母/数字/下划线");
        }
        if (password == null || password.length() < 6 || password.length() > 64) {
            throw new BizException(ErrorCode.BAD_REQUEST, "密码长度需在 6-64 位之间");
        }
        if (!"ADMIN".equals(role) && !"MEMBER".equals(role)) {
            role = "MEMBER";
        }
        Long existing = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (existing > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
        return UserVO.from(user);
    }

    @Transactional
    public UserVO update(Long operatorId, Long id, String realName, String phone, Integer status,
                         String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        if (StringUtils.hasText(realName)) {
            user.setRealName(realName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }
        if (status != null) {
            if (user.getId().equals(operatorId) && status == 0) {
                throw new BizException(ErrorCode.BAD_REQUEST, "不能禁用自己的账号");
            }
            user.setStatus(status);
        }
        if (StringUtils.hasText(newPassword)) {
            if (newPassword.length() < 6 || newPassword.length() > 64) {
                throw new BizException(ErrorCode.BAD_REQUEST, "密码长度需在 6-64 位之间");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    @Transactional
    public void delete(Long operatorId, Long id) {
        if (operatorId.equals(id)) {
            throw new BizException(ErrorCode.CANNOT_DELETE_SELF);
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        userMapper.deleteById(id);
    }
}
