package com.saas.crm.followup.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("follow_up_record")
public class FollowUp {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String content;

    /** 本次跟进得出的意向结论 */
    private String resultLevel;

    private LocalDateTime nextFollowupAt;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
