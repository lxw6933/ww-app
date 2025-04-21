package com.ww.app.pay.enums;

import lombok.Getter;

/**
 * 支付方式枚举
 */
@Getter
public enum PayTypeEnum {
    /**
     * APP支付
     */
    APP("app", "APP支付"),
    /**
     * H5支付
     */
    WAP("wap", "H5支付"),
    /**
     * PC网页支付
     */
    PC("pc", "PC网页支付"),
    /**
     * 扫码支付(用户扫商家)
     */
    NATIVE("native", "扫码支付"),
    /**
     * 小程序支付
     */
    MINI_PROGRAM("mini_program", "小程序支付"),
    /**
     * 公众号支付
     */
    JSAPI("jsapi", "公众号支付");

    private final String code;
    private final String desc;

    PayTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static PayTypeEnum getByCode(String code) {
        for (PayTypeEnum typeEnum : values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum;
            }
        }
        return null;
    }
} 