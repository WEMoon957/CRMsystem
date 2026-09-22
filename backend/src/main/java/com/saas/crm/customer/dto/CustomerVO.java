package com.saas.crm.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerVO {

    private Long id;
    private String name;
    private String phone;
    private String company;
    private String position;
    private String source;
    private String industry;
    private String intentLevel;
    private String intentLabel;
    private String intentTone;
    private String address;
    private String remark;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime nextFollowupAt;
    private LocalDateTime lastContactedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
