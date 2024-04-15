package com.ww.mall.seckill.service;

/**
 * @author ww
 * @create 2024-02-06- 14:55
 * @description:
 */
public interface SeckillService {

    boolean seckillOrder();

    void traceId();

    void msg();

    void cache(String msg);

    void boomFilter();

    void liteFlow();
}

