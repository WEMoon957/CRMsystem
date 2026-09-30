package com.saas.crm.common.exception;

import com.saas.crm.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：客户端永远收到结构化错误，绝不暴露堆栈与内部细节。
 * 业务错误走 ApiResponse（HTTP 200 + 业务码），与前端拦截器约定一致。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException e) {
        return ResponseEntity.ok(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /** 请求体校验失败（@Valid）/ 表单绑定失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.BAD_REQUEST.getDefaultMessage());
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), msg));
    }

    /** 缺少必填请求参数 / 参数类型不匹配 / 请求体不可读 */
    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
        log.warn("bad request: {}", e.getMessage());
        return ResponseEntity.ok(ApiResponse.error(
                ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getDefaultMessage()));
    }

    /** 上传超出 multipart 限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.ok(ApiResponse.error(
                ErrorCode.FILE_TOO_LARGE.getCode(), ErrorCode.FILE_TOO_LARGE.getDefaultMessage()));
    }

    /** 静态资源/路径不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.ok(ApiResponse.error(
                ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getDefaultMessage()));
    }

    /** 请求方法不支持（如对 POST 端点发 GET） */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.ok(ApiResponse.error(
                ErrorCode.METHOD_NOT_ALLOWED.getCode(), ErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("unexpected server error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL.getCode(), "服务器开小差了，请稍后重试"));
    }
}
