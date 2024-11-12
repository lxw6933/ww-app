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
    public static final String MALL_CREATE_ORDER_KEY = "mall.create.order.key";

    /**
     * test key
     */
    public static final String MALL_COUPON_TEST_KEY = "mall.coupon.test.key";

    /**
     * 用户注册key
     */
    public static final String MALL_MEMBER_REGISTER_KEY = "mall.member.register.key";

    /**
     * canal route key
     */
    public static final String MALL_CANAL_KEY = "mall.canal.key";

    /**
     * 关单key
     */
    public static final String MALL_OMS_CLOSE_KEY = "mall.oms.close.key";

    /**
     * 延时15分钟关单死信key
     */
    public static final String MALL_OMS_DELAY_FIFTEEN_KEY = "mall.oms.delay.15.key";

    /**
     * 商品定时上架key
     */
    public static final String MALL_PRODUCT_TIMER_UP_KEY = "mall.product.timer.up.key";

}
