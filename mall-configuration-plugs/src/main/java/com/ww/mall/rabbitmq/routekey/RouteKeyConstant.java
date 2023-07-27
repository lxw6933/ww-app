package com.ww.mall.rabbitmq.routekey;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 23:03
 **/
public class RouteKeyConstant {

    private RouteKeyConstant() {}

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

}
