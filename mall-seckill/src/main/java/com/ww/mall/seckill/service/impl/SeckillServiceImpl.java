package com.ww.mall.seckill.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
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
import java.util.concurrent.atomic.AtomicInteger;

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

    private final AtomicInteger num = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        redisTemplate.opsForValue().set("skuStock", "1000");
    }

    @Override
    public boolean seckillOrder() {
        // 校验用户是否存在秒杀资格
//        MallClientUser clientUser = AuthorizationContext.getClientUser();
        // 本地缓存存储活动信息，校验活动信息
        // 本地缓存商品信息，校验商品信息
        if (mallRedisUtil.decrementStock("skuStock", 1) >= 0) {
            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
            String orderNo = IdUtil.generatorIdStr();
            int totalOrderNum = num.incrementAndGet();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
            log.info("下单总数【{}】订单【{}】下单成功【{}】", totalOrderNum, orderNo, orderDate);
        }
        return true;
    }
}
