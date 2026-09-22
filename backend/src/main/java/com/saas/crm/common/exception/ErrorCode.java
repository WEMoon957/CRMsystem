package com.saas.crm.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码体系：按区间划分模块。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用 1xxx
    BAD_REQUEST(1000, "请求参数不合法"),
    NOT_FOUND(1001, "资源不存在"),
    INTERNAL(1002, "服务器内部错误"),
    FORBIDDEN(1003, "无权访问该资源"),

    // 认证 2xxx
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    USERNAME_OR_PASSWORD_ERROR(2002, "用户名或密码错误"),
    ACCOUNT_DISABLED(2003, "账号已被禁用"),
    REFRESH_TOKEN_INVALID(2004, "刷新令牌无效或已过期"),
    USERNAME_EXISTS(2005, "用户名已存在"),

    // 用户 3xxx
    CANNOT_DELETE_SELF(3001, "不能删除自己的账号"),

    // 客户 4xxx
    CUSTOMER_NOT_FOUND(4001, "客户不存在"),
    CUSTOMER_NOT_OWNED(4002, "只能操作自己名下的客户");

    private final int code;
    private final String defaultMessage;
}
