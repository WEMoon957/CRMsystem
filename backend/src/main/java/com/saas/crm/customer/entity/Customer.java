package com.saas.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String phone;

    private String company;

    private String position;

    private String source;

    private String industry;

    /** 意向等级，见 IntentLevel */
    private String intentLevel;

    private String address;

    private String remark;

    /** 客户归属人：子账号只能看到/操作自己名下的客户 */
    private Long ownerId;

    private LocalDateTime nextFollowupAt;

    private LocalDateTime lastContactedAt;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
