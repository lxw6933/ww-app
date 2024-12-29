package com.ww.mall.order.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-08-11- 11:36
 * @description: 活动类型
 */
@Getter
public enum ActivityType {

    LOTTERY("抽奖"),
    CHANNEL_REPLACE_BUY("渠道换购"),
    MERCHANT_REPLACE_BUY("商家换购"),
    CHANNEL_PRESELL("渠道预售"),
    MERCHANT_PRESELL("商家预售"),
    NORMAL("正常"),
    LIMITED_BUY("限时购"),
    COMBINATION_PURCHASE("组合购"),
    NM("N选M活动"),
    CONVERT_CODE("兑换码"),
    EQUITY("权益领取");

    private final String text;

    ActivityType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
