package com.saas.crm.common.exception;

import lombok.Getter;

/**
 * 业务异常：service 层抛出，由全局异常处理器统一转换为 ApiResponse。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorMessage(errorCode));
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    private static String errorMessage(ErrorCode errorCode) {
        return errorCode.getDefaultMessage();
    }
}
