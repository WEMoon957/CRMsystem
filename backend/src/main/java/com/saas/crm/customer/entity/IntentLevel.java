package com.saas.crm.customer.entity;

import com.saas.crm.common.exception.BizException;
import com.saas.crm.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户意向五档。
 */
@Getter
@AllArgsConstructor
public enum IntentLevel {

    PENDING("待跟进", "info"),
    INTERESTED("有意向", "success"),
    NOT_INTERESTED("无意向", "warning"),
    COOPERATED("已合作", "primary"),
    REFUSED("不合作", "danger");

    private final String label;
    /** 前端展示用的语义色 */
    private final String tone;

    public static IntentLevel of(String value) {
        for (IntentLevel level : values()) {
            if (level.name().equals(value)) {
                return level;
            }
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "非法的意向等级: " + value);
    }
}
