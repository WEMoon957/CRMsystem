package com.saas.crm.customer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_attachment")
public class Attachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;

    private String fileName;

    private String fileType;

    private String filePath;

    private Long fileSize;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
