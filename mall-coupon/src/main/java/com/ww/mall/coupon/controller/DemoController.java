package com.ww.mall.coupon.controller;

import com.ww.mall.common.exception.ApiException;
import com.ww.mall.coupon.config.CouponProperties;
import com.ww.mall.coupon.dao.CouponMapper;
import com.ww.mall.coupon.service.CouponService;
import com.ww.mall.coupon.view.bo.CouponPageBO;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.redis.MallRedisUtil;
import com.ww.mall.redis.annotation.MallDistributedLock;
import com.ww.mall.web.config.SecretProperties;
import com.ww.mall.web.config.ip2region.Ip2regionSearcher;
import com.ww.mall.web.config.thread.DefaultThreadPoolProperties;
import com.ww.mall.web.utils.VerificationCodeUtil;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author:         ww
 * Datetime:       2021\3\4 0004
 * Description:
 */
@Slf4j
@RestController
@RequestMapping("/coupon")
public class DemoController {

    @Autowired
    private DefaultThreadPoolProperties defaultThreadPoolProperties;

    @Autowired
    private SecretProperties secretProperties;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CouponProperties couponProperties;

    @Autowired
    private Ip2regionSearcher ip2regionSearcher;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CouponService couponService;

    @Autowired
    private MallPublisher mallPublisher;

    @Autowired
    private CouponMapper couponMapper;

    @GetMapping("/testMsg")
    public void testMsg(String msg) {
        mallPublisher.publishMsg(ExchangeConstant.MALL_COUPON_EXCHANGE, RouteKeyConstant.MALL_COUPON_TEST_KEY, msg);
    }

    private final AtomicInteger num = new AtomicInteger(0);

    @Autowired
    private MallRedisUtil mallRedisUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        redisTemplate.opsForValue().set("wwStock", "10");
        System.out.println("初始化库存10");
    }

    @GetMapping("/redisStock")
    public void redisStock() {
        if (mallRedisUtil.decrementStock("wwStock", 1) >= 0) {
            log.info("stock");
            int count = num.getAndIncrement();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, count);
        }
    }

    @GetMapping("/lineLock")
    public void lineLock(@RequestParam("activityCode") String activityCode) {
        int total = num.getAndIncrement();
        if (total > 500000) {
            throw new ApiException("库存不足");
        }
//        RLock lock = redissonClient.getLock(activityCode);
//        try {
//            lock.lock(10, TimeUnit.SECONDS);
//
//        } catch (Exception e) {
//            throw new ApiException("服务异常");
//        } finally {
//            lock.unlock();
//        }


    }

    @GetMapping("/lock")
    public void getLock(CouponPageBO couponPageBO) {
        couponService.demo(couponPageBO);
    }

    @GetMapping("/demo/add/{id}")
    public void add(@PathVariable("id") String id) {
        stringRedisTemplate.opsForHyperLogLog().add("ww", id);
    }

    @GetMapping("/demo/size")
    public int add() {
        return stringRedisTemplate.opsForHyperLogLog().size("ww").intValue();
    }

    @RequestMapping("/demo")
    public String demo(HttpServletRequest request){
//        CompletableFuture.runAsync(() -> {
//            log.info("子线程打印");
//        }, defaultThreadPoolExecutor);
        log.info("main线程执行");
        return "coupon active is opening！！！" + couponProperties;
    }

    @RequestMapping("/test")
    public String test(){
        String code = VerificationCodeUtil.generateVerificationCode(4);
        String key = "test_redis_rate:" + code;
        stringRedisTemplate.opsForValue().set(key, code, 3, TimeUnit.MINUTES);
        return "success";
    }


}

