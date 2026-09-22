package com.saas.crm.customer.service;

import com.saas.crm.auth.mapper.UserMapper;
import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import com.saas.crm.customer.dto.CustomerVO;
import com.saas.crm.customer.entity.Customer;
import com.saas.crm.customer.mapper.CustomerMapper;
import com.saas.crm.followup.mapper.FollowUpMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CustomerService 数据隔离守卫测试：
 * 子账号（MEMBER）绝不能读写他人名下客户，主账号（ADMIN）不受限。
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FollowUpMapper followUpMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer customerOwnedBy(long ownerId) {
        Customer customer = new Customer();
        customer.setId(99L);
        customer.setName("张伟");
        customer.setOwnerId(ownerId);
        customer.setIntentLevel("PENDING");
        return customer;
    }

    @Test
    void memberCannotReadCustomerOwnedByOthers() {
        when(customerMapper.selectById(99L)).thenReturn(customerOwnedBy(2L));

        BizException e = assertThrows(BizException.class,
                () -> customerService.detail(1L, false, 99L));

        assertEquals(ErrorCode.CUSTOMER_NOT_OWNED.getCode(), e.getCode());
    }

    @Test
    void memberCannotDeleteCustomerOwnedByOthers() {
        when(customerMapper.selectById(99L)).thenReturn(customerOwnedBy(2L));

        BizException e = assertThrows(BizException.class,
                () -> customerService.delete(1L, false, 99L));

        assertEquals(ErrorCode.CUSTOMER_NOT_OWNED.getCode(), e.getCode());
    }

    @Test
    void memberCanReadOwnCustomer() {
        when(customerMapper.selectById(99L)).thenReturn(customerOwnedBy(1L));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        CustomerVO vo = customerService.detail(1L, false, 99L);

        assertEquals(99L, vo.getId());
        assertEquals("张伟", vo.getName());
    }

    @Test
    void adminCanReadAnyCustomer() {
        when(customerMapper.selectById(99L)).thenReturn(customerOwnedBy(2L));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());

        CustomerVO vo = customerService.detail(1L, true, 99L);

        assertEquals(99L, vo.getId());
    }

    @Test
    void missingCustomerYieldsNotFound() {
        when(customerMapper.selectById(404L)).thenReturn(null);

        BizException e = assertThrows(BizException.class,
                () -> customerService.detail(1L, true, 404L));

        assertEquals(ErrorCode.CUSTOMER_NOT_FOUND.getCode(), e.getCode());
    }

    @Test
    void memberCannotAssignCustomers() {
        BizException e = assertThrows(BizException.class,
                () -> customerService.assign(1L, false, 99L, 2L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), e.getCode());
    }
}
