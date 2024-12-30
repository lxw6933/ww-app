package com.ww.app.order.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-08-11- 13:37
 * @description:
 */
@Getter
public enum RechargeType {

    CARD_PASSWORD("卡密"),
    CARD_CODE("卡券"),
    DIRECT_RECHARGE("直充"),
    SHORT_CHAIN("短链"),
    TICKET_CODE("券码");

    private final String text;

    RechargeType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
