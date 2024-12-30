package com.ww.app.seckill.service;

import com.ww.app.seckill.view.bo.SecKillOrderReqBO;

import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-02-06- 14:55
 * @description:
 */
public interface SeckillService {

    /**
     * 秒杀验证码获取
     *
     * @param response 响应
     * @param activityCode 活动编码
     * @param skuId 秒杀商品
     */
    void captcha(HttpServletResponse response, String activityCode, Long skuId);

    /**
     * 秒杀路径获取
     *
     * @param activityCode 活动编码
     * @param skuId 秒杀商品
     */
    String getSecKillPath(String activityCode, Long skuId);

    /**
     * 秒杀商品
     *
     * @param secKillPath 秒杀路径
     * @param secKillOrderReqBO 秒杀请求
     */
    Boolean doSecKillOrder(String secKillPath, SecKillOrderReqBO secKillOrderReqBO);

}

