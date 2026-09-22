package com.saas.crm.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称过长")
    private String productName;

    @Size(max = 128, message = "规格型号过长")
    private String spec;

    private Integer quantity;

    private BigDecimal unitPrice;

    @Size(max = 512, message = "备注过长")
    private String remark;
}
