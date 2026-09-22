package com.saas.crm.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增/修改客户请求体。
 */
@Data
public class CustomerSaveRequest {

    @NotBlank(message = "客户姓名不能为空")
    @Size(max = 64, message = "客户姓名过长")
    private String name;

    @Pattern(regexp = "^$|1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Size(max = 128, message = "公司名称过长")
    private String company;

    @Size(max = 64, message = "职位过长")
    private String position;

    @Size(max = 32, message = "客户来源过长")
    private String source;

    @Size(max = 64, message = "行业过长")
    private String industry;

    @NotBlank(message = "意向等级不能为空")
    @Pattern(regexp = "PENDING|INTERESTED|NOT_INTERESTED|COOPERATED|REFUSED", message = "意向等级非法")
    private String intentLevel;

    @Size(max = 255, message = "地址过长")
    private String address;

    @Size(max = 1024, message = "备注过长")
    private String remark;

    /** 仅主账号可指定归属人；子账号新增时自动归属自己 */
    private Long ownerId;

    private LocalDateTime nextFollowupAt;
}
