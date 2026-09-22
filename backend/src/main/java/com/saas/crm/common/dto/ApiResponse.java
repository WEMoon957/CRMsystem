package com.saas.crm.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一 API 响应结构。
 * code = 0 表示成功；非 0 为业务错误码。
 */
@Data
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
