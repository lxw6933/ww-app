package com.ww.mall.coupon.controller;

import com.ww.mall.coupon.config.CouponProperties;
import com.ww.mall.coupon.service.CouponService;
import com.ww.mall.coupon.view.bo.CouponPageBO;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.web.config.SecretProperties;
import com.ww.mall.web.config.ip2region.Ip2regionSearcher;
import com.ww.mall.web.config.thread.DefaultThreadPoolProperties;
import com.ww.mall.web.utils.VerificationCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

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

    @GetMapping("/testMsg")
    public void testMsg(String msg) {
        mallPublisher.publishMsg(ExchangeConstant.MALL_COUPON_EXCHANGE, RouteKeyConstant.MALL_COUPON_TEST_KEY, msg);
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

