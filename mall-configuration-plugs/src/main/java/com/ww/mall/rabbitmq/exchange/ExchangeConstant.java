package com.ww.mall.rabbitmq.exchange;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:00
 **/
public class ExchangeConstant {

    private ExchangeConstant() {}

    /**
     * 通用【定制】延时交换机
     * 注：时间最好不要超过40+天（long类型的最大值对应的天数）
     * rabbitmq 添加 延时插件才能生效
     * 插件地址：https://www.rabbitmq.com/community-plugins.html
     * 插件名称：rabbitmq_delayed_message_exchange
     * 插件下载地址：rabbitmq/rabbitmq-delayed-message-exchange
     */
    public static final String MALL_COMMON_DELAY_EXCHANGE = "mall.common.delay.exchange";

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
