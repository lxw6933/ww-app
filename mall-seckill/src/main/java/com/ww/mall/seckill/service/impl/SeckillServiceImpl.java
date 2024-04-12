package com.ww.mall.seckill.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ww.mall.common.constant.RedisChannelConstant;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.redis.MallRedisUtil;
import com.ww.mall.seckill.service.SeckillService;
import com.ww.mall.web.feign.ThirdServerFeignService;
import com.ww.mall.web.utils.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
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
    private ThirdServerFeignService thirdServerFeignService;

    @Autowired
    private ThreadPoolExecutor defaultThreadPoolExecutor;

    @Autowired
    private MallRedisUtil mallRedisUtil;

    @Autowired
    private MallPublisher mallPublisher;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final AtomicInteger num = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("filter:");
        bloomFilter.tryInit(defaultInitSize, defaultErrorProbability);

        redisTemplate.opsForValue().set("skuStock", "1000");
        // 初始化活动数据信息
        activityCache.get("activityRedisCacheKey", key -> redisTemplate.opsForValue().get(key));
    }

    private static final Cache<String, String> activityCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(10000)
            .build();

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

    @Override
    public void traceId() {
        // interface 日志
        log.info("interface start log");
        // thread pool日志
        for (int i = 0; i < 3; i++) {
            defaultThreadPoolExecutor.submit(() -> log.info("thread pool log"));
        }
        // mq日志
        mallPublisher.publishMsg(ExchangeConstant.MALL_MEMBER_EXCHANGE, RouteKeyConstant.MALL_MEMBER_REGISTER_KEY, 1);
        // feign日志
        thirdServerFeignService.sendSms("15970191157", "9527");
        log.info("interface end log");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void msg() {
        log.info("seckill msg");
        rabbitTemplate.convertAndSend(QueueConstant.MALL_TEST_QUEUE, "1");
    }

    @Override
    public void cache(String msg) {
        mallRedisUtil.publishMessage(RedisChannelConstant.MALL_SPU_CHANNEL, msg);
    }

    @Autowired
    private RedissonClient redissonClient;

    private static final Long defaultInitSize = 100000000L;
    private static final Double defaultErrorProbability = 0.01;

    private RBloomFilter<String> bloomFilter;

    @Override
    public void boomFilter() {
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        for (int i = 0; i < defaultInitSize / 10; i++) {
            String code;
            if (i % 2 == 0) {
                code = RandomUtil.randomString(8);
            } else {
                code = RandomUtil.randomString(16);
            }
            executorService.submit(() -> {
                System.out.println(code);
                bloomFilter.add(code);
            });
        }

    }

}
