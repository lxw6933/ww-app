package com.ww.mall.rabbitmq.routekey;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:03
 **/
public class RouteKeyConstant {

    private RouteKeyConstant() {}

    /**
     * 创建订单 key
     */
    public static final String CREATE_ORDER_KEY = "create.order.key";

    /**
     * test key
     */
    public static final String COUPON_TEST_KEY = "coupon.test.key";

    /**
     * 用户注册key
     */
    public static final String MEMBER_REGISTER_KEY = "member.register.key";

    /**
     * canal route key
     */
    public static final String CANAL_KEY = "canal.key";

    /**
     * 关单key
     */
    public static final String OMS_CLOSE_KEY = "oms.close.key";

    /**
     * 延时15分钟关单死信key
     */
    public static final String OMS_DELAY_FIFTEEN_KEY = "oms.delay.15.key";

    /**
     * 商品定时上架key
     */
    public static final String PRODUCT_TIMER_UP_KEY = "product.timer.up.key";

}
