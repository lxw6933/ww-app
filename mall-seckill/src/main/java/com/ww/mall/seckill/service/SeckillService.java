package com.ww.mall.seckill.service;

import com.ww.mall.seckill.view.bo.SecKillOrderReqBO;

/**
 * @author ww
 * @create 2024-02-06- 14:55
 * @description:
 */
public interface SeckillService {

    String getSecKillPath(String activityCode, Long skuId);

    Boolean doSecKillOrder(String secKillPath, SecKillOrderReqBO secKillOrderReqBO);

    boolean seckillOrder();

    void traceId();

    void msg();

    void cache(String msg);

    void boomFilter();

    void liteFlow();
}

