package com.ww.app.coupon.controller;

import com.ww.app.common.exception.ApiException;
import com.ww.app.coupon.config.CouponProperties;
import com.ww.app.coupon.service.CouponService;
import com.ww.app.coupon.view.bo.CouponPageBO;
import com.ww.app.rabbitmq.RabbitMqPublisher;
import com.ww.app.rabbitmq.exchange.ExchangeConstant;
import com.ww.app.rabbitmq.routekey.RouteKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CouponProperties couponProperties;

    @Autowired
    private CouponService couponService;

    @Autowired
    private RabbitMqPublisher rabbitMqPublisher;

    @GetMapping("/testMsg")
    public void testMsg(String msg) {
        rabbitMqPublisher.sendMsg(ExchangeConstant.COUPON_EXCHANGE, RouteKeyConstant.COUPON_TEST_KEY, msg);
    }

    private final AtomicInteger num = new AtomicInteger(0);

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

}

