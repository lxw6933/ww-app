package com.ww.mall.rabbitmq.exchange;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:00
 **/
public class ExchangeConstant {

    private ExchangeConstant() {}

    /**
     * 会员服务交换机
     */
    public static final String MALL_MEMBER_EXCHANGE = "mall.member.exchange";

    /**
     * canal exchange
     */
    public static final String MALL_CANAL_EXCHANGE = "mall.canal.exchange";

    /**
     * oms exchange
     */
    public static final String MALL_OMS_EXCHANGE = "mall.oms.exchange";

}
