package com.ww.app.pay.enums;

import lombok.Getter;

/**
 * 支付渠道枚举
 */
@Getter
public enum PayChannelEnum {
    /**
     * 支付宝
     */
    ALIPAY("alipay", "支付宝"),
    /**
     * 微信支付
     */
    WXPAY("wxpay", "微信支付"),
    /**
     * 微信支付V3
     */
    WXPAY_V3("wxpay_v3", "微信支付V3");

    private final String code;
    private final String desc;

    PayChannelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据code获取枚举
     */
    public static PayChannelEnum getByCode(String code) {
        for (PayChannelEnum channelEnum : values()) {
            if (channelEnum.getCode().equals(code)) {
                return channelEnum;
            }
        }
        return null;
    }
} 