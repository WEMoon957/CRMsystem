package com.saas.crm.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String role;
    private Integer status;
    private Integer customerCount;

    public static UserVO from(com.saas.crm.auth.entity.User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
