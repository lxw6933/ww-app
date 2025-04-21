package com.ww.app.pay.enums;

/**
 * 支付状态枚举
 */
public enum PayStatusEnum {
    /**
     * 待支付
     */
    WAIT_PAY(0, "待支付"),
    /**
     * 支付中
     */
    PAYING(1, "支付中"),
    /**
     * 支付成功
     */
    PAY_SUCCESS(2, "支付成功"),
    /**
     * 支付失败
     */
    PAY_FAILED(3, "支付失败"),
    /**
     * 已关闭
     */
    CLOSED(4, "已关闭"),
    /**
     * 已退款
     */
    REFUNDED(5, "已退款"),
    /**
     * 部分退款
     */
    PARTIAL_REFUNDED(6, "部分退款");

    private final int code;
    private final String desc;

    PayStatusEnum(int code, String desc) {
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
    public static PayStatusEnum getByCode(int code) {
        for (PayStatusEnum statusEnum : values()) {
            if (statusEnum.getCode() == code) {
                return statusEnum;
            }
        }
        return null;
    }
} 