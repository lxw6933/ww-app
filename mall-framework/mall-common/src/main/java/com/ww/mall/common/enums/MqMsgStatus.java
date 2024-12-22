package com.ww.mall.common.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:39
 **/
@Getter
public enum MqMsgStatus {

    DELIVER_SUCCESS("发送成功"),
    DELIVER_FAIL("发送失败"),
    CONSUMED_SUCCESS("消费成功"),
    CONSUMED_FAIL("消费失败");

    private final String text;

    MqMsgStatus(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
