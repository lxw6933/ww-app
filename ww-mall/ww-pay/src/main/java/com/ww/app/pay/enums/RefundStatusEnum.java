package com.ww.app.pay.enums;

/**
 * 退款状态枚举
 */
public enum RefundStatusEnum {
    /**
     * 退款中
     */
    REFUNDING(0, "退款中"),
    /**
     * 退款成功
     */
    REFUND_SUCCESS(1, "退款成功"),
    /**
     * 退款失败
     */
    REFUND_FAILED(2, "退款失败");

    private final int code;
    private final String desc;

    RefundStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据code获取枚举
     */
    public static RefundStatusEnum getByCode(int code) {
        for (RefundStatusEnum statusEnum : values()) {
            if (statusEnum.getCode() == code) {
                return statusEnum;
            }
        }
        return null;
    }
} 