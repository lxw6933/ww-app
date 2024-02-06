package com.ww.mall.seckill.service.impl;

import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.redis.MallRedisUtil;
import com.ww.mall.seckill.service.SeckillService;
import com.ww.mall.web.utils.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @author ww
 * @create 2024-02-06- 14:55
 * @description:
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private MallRedisUtil mallRedisUtil;

    @Autowired
    private MallPublisher mallPublisher;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        redisTemplate.opsForValue().set("skuStock", "10");
    }

    @Override
    public boolean seckillOrder() {
        if (mallRedisUtil.decrementStock("skuStock", 1) >= 0) {
            Date orderDate = new Date();
            String orderNo = IdUtil.generatorIdStr();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
            log.info("订单【{}】下单成功【{}】", orderNo, orderDate);
        }
        return true;
    }
}
