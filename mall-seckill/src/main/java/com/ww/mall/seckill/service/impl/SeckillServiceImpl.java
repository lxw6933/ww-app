package com.ww.mall.seckill.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.MD5;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wf.captcha.ArithmeticCaptcha;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.common.constant.RedisChannelConstant;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.common.utils.IdUtil;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.redis.MallRedisTemplate;
import com.ww.mall.seckill.entity.SecKillOrder;
import com.ww.mall.seckill.manager.MallCacheManager;
import com.ww.mall.seckill.node.executor.DemoFlowExecutor;
import com.ww.mall.seckill.service.SeckillService;
import com.ww.mall.seckill.view.bo.SecKillOrderReqBO;
import com.ww.mall.web.feign.ThirdServerFeignService;
import com.ww.mall.web.utils.AuthorizationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    private MallRedisTemplate mallRedisTemplate;

    @Autowired
    private MallPublisher mallPublisher;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final AtomicInteger num = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        redisTemplate.opsForValue().set("skuStock", "1000");
        // 初始化活动数据信息
        activityCache.get("activityRedisCacheKey", key -> redisTemplate.opsForValue().get(key));
    }

    private static final Cache<String, String> activityCache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(10000)
            .build();

    @Override
    public void captcha(HttpServletResponse response, String activityCode, Long skuId) {
        // 获取用户
        MallClientUser clientUser = AuthorizationContext.getClientUser();
//        // 算术
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        captcha.getArithmeticString();
//        // gif类型
//        GifCaptcha captcha = new GifCaptcha(130, 48);
//        // 中文类型
//        ChineseCaptcha captcha = new ChineseCaptcha(130, 48);
//        // 中文gif类型
//        ChineseGifCaptcha captcha = new ChineseGifCaptcha(130, 48);
        captcha.setLen(3);
        captcha.text();
        // 验证码结果存入redis
        String key = getSecKillVerCodeKey(clientUser, activityCode, skuId);
        redisTemplate.opsForValue().set(key, captcha.text(), 1, TimeUnit.MINUTES);
        // 输出图片流
        try {
            captcha.out(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSecKillPath(String activityCode, Long skuId) {
        // 获取用户
        MallClientUser clientUser = AuthorizationContext.getClientUser();

        String key = getSecKillPathKey(clientUser, activityCode, skuId);
        String userSecKillPath = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(userSecKillPath)) {
            return userSecKillPath;
        }
        // 生成secKillPath
        userSecKillPath = MD5.create().digestHex(UUID.randomUUID() + activityCode + clientUser.getMemberId().toString(), "UTF-8");
        // 加密后地址存入redis
        redisTemplate.opsForValue().set(key, userSecKillPath, 1, TimeUnit.MINUTES);
        return userSecKillPath;
    }

    @Override
    public Boolean doSecKillOrder(String userSecKillPath, SecKillOrderReqBO reqBO) {
        // 校验用户是否存在秒杀资格
        MallClientUser clientUser = AuthorizationContext.getClientUser();
        // 校验地址是否正确
        Assert.isFalse(checkSecKillPath(clientUser, userSecKillPath, reqBO.getActivityCode(), reqBO.getSkuId()), () -> new ApiException("秒杀路径异常"));
        // 校验图形验证码是否正确
        Assert.isFalse(checkSecKillVerCode(clientUser, reqBO.getActivityCode(), reqBO.getSkuId(), reqBO.getCaptcha()), () -> new ApiException("验证码错误"));
        // 本地缓存存储活动信息，校验活动信息

        // 本地缓存商品信息，校验商品信息
        if (mallRedisTemplate.decrementStock("skuStock", 1) >= 0) {
            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
            String orderNo = IdUtil.generatorIdStr();
            int totalOrderNum = num.incrementAndGet();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
            log.info("下单总数【{}】订单【{}】下单成功【{}】", totalOrderNum, orderNo, orderDate);
            return true;
        }
        return false;
    }

    private boolean checkSecKillPath(MallClientUser clientUser, String userSecKillPath, String activityCode, Long skuId) {
        if (StringUtils.isEmpty(userSecKillPath)) {
            return false;
        }
        String key = getSecKillPathKey(clientUser, activityCode, skuId);
        String userSecKillPathCache = redisTemplate.opsForValue().get(key);
        return userSecKillPath.equals(userSecKillPathCache);
    }

    private boolean checkSecKillVerCode(MallClientUser clientUser, String activityCode, Long skuId, String userVerCode) {
        String key = getSecKillVerCodeKey(clientUser, activityCode, skuId);
        String verCodeCache = redisTemplate.opsForValue().get(key);
        return userVerCode.equals(verCodeCache);
    }

    private String getSecKillPathKey(MallClientUser clientUser, String activityCode, Long skuId) {
        return RedisKeyConstant.SECKILL_PATH_PREFIX + clientUser.getMemberId() + RedisKeyConstant.SPLIT_KEY + activityCode + RedisKeyConstant.SPLIT_KEY + skuId;
    }

    private String getSecKillVerCodeKey(MallClientUser clientUser, String activityCode, Long skuId) {
        return RedisKeyConstant.SECKILL_CODE_PREFIX + clientUser.getMemberId() + RedisKeyConstant.SPLIT_KEY + activityCode + RedisKeyConstant.SPLIT_KEY + skuId;
    }

    @Override
    public boolean seckillOrder() {
        // 校验用户是否存在秒杀资格
//        MallClientUser clientUser = AuthorizationContext.getClientUser();
        // 本地缓存存储活动信息，校验活动信息
        // 本地缓存商品信息，校验商品信息
        if (mallRedisTemplate.decrementStock("skuStock", 1) >= 0) {
            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
            String orderNo = IdUtil.generatorIdStr();
            int totalOrderNum = num.incrementAndGet();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
            log.info("下单总数【{}】订单【{}】下单成功【{}】", totalOrderNum, orderNo, orderDate);
        }
//        if (mallRedisTemplate.decrementStock2("skuStock", 1) >= 0) {
//            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
//            String orderNo = IdUtil.generatorIdStr();
//            int totalOrderNum = num.incrementAndGet();
//            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
//            log.info("下单总数【{}】订单【{}】下单成功【{}】", totalOrderNum, orderNo, orderDate);
//        }
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
        mallRedisTemplate.publishMessage(RedisChannelConstant.MALL_SPU_CHANNEL, msg);
    }

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void boomFilter() {
        SecKillOrder secKillOrder = new SecKillOrder();
        secKillOrder.setUserId(0L);
        secKillOrder.setOrderType(0);
        secKillOrder.setOrderNo(IdUtil.generatorIdStr());
        secKillOrder.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        mongoTemplate.save(secKillOrder);
        MallCacheManager.spuCache.get("1", res -> null);
        log.info("执行完毕filter数量");
    }

    @Autowired
    private DemoFlowExecutor demoFlowExecutor;

    @Override
    public void liteFlow() {
        demoFlowExecutor.testConfig();
    }

}
