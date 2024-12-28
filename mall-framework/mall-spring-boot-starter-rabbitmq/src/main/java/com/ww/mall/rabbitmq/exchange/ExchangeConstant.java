package com.ww.mall.rabbitmq.exchange;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:00
 **/
public class ExchangeConstant {

    private ExchangeConstant() {}

    /**
     * 缓存更新通知广播交换机
     */
    public static final String CACHE_NOTICE_FANOUT_EXCHANGE = "cache.notice.fanout.exchange";

    /**
     * 通用【定制】延时交换机
     * 注：时间最好不要超过40+天（long类型的最大值对应的天数）
     * rabbitmq 添加 延时插件才能生效
     * 插件地址：https://www.rabbitmq.com/community-plugins.html
     * 插件名称：rabbitmq_delayed_message_exchange
     * 插件下载地址：rabbitmq/rabbitmq-delayed-message-exchange
     * 将插件放入rabbitmq-server里的plugins包里
     * 执行 rabbitmq-plugins enable rabbitmq_delayed_message_exchange 重启
     */
    public static final String COMMON_DELAY_EXCHANGE = "common.delay.exchange";

    /**
     * 优惠券服务交换机
     */
    public static final String COUPON_EXCHANGE = "coupon.exchange";

    /**
     * 会员服务交换机
     */
    public static final String MEMBER_EXCHANGE = "member.exchange";

    /**
     * canal exchange
     */
    public static final String CANAL_EXCHANGE = "canal.exchange";

    /**
     * oms exchange
     */
    public static final String OMS_EXCHANGE = "oms.exchange";

}
