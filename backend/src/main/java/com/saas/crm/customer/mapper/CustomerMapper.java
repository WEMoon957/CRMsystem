package com.saas.crm.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.saas.crm.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
