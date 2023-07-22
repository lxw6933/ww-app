package com.ww.mall.enums;

import java.util.StringJoiner;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:39
 **/
public enum MqMsgStatus {

    DELIVER_SUCCESS("发送成功"),
    DELIVER_FAIL("发送失败"),
    CONSUMED_SUCCESS("消费成功"),
    CONSUMED_FAIL("消费失败");

    private String text;

    MqMsgStatus(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
