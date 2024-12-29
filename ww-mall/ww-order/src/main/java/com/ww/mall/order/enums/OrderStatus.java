package com.ww.mall.order.enums;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-08-11- 09:40
 * @description: 主订单状态
 */
@Getter
public enum OrderStatus {

    WAIT_PAY_DEPOSIT("待付定金"),
    WAIT_PAY_FINAL_PAYMENT("待付尾款"),
    WAIT_PAY("待付款"),
    DELIVERY("待发货"),
    RECEIVE("待收货"),
    ESTIMATE("待评价"),
    COMPLETE("已完成"),
    CLOSE("交易关闭"),
    CANCEL("已取消");

    private final String text;

    OrderStatus(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
