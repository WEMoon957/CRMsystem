package com.saas.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("customer_product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String productName;

    private String spec;

    private Integer quantity;

    private BigDecimal unitPrice;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
